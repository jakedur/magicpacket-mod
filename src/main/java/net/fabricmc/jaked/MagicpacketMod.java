package net.fabricmc.jaked;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.fabricmc.api.ModInitializer;
import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MagicpacketMod implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger("modid");

	// Load config 'magicpacket.properties', if it isn't present create one
	SimpleConfig CONFIG = SimpleConfig.of( "magicpacket" ).provider( this::provider ).request();

	// Custom config provider, returns the default config content
	// if the custom provider is not specified SimpleConfig will create an empty file instead
	private String provider( String filename ) {
		return "MAC-Address=\nBroadcast-IP=255.255.255.255\nDiscord-Token=\nDiscord-Server-ID=\nDiscord-Channel-ID=\nDiscord-Wait-TimeMS=10000";
	}

	public final String MAC = CONFIG.getOrDefault( "MAC-Address", "" );
	public final String BROADCAST = CONFIG.getOrDefault( "Broadcast-IP", "255.255.255.255" );
	public final String DToken = CONFIG.getOrDefault( "Discord-Token", "" );
	public final String DServerID = CONFIG.getOrDefault( "Discord-Server-ID", "" );
	public final String DChannelID = CONFIG.getOrDefault( "Discord-Channel-ID", "" );
	public final int DWait = CONFIG.getOrDefault( "Discord-Wait-TimeMS", 10000);

	@Override
	public void onInitialize() {
		LOGGER.info("Starting magic packet sender");

		if(!MAC.equals("")) {
			byte[] macBytes = new byte[6]; // convert mac to bytes
			String[] hex = MAC.split("(\\:|\\-)");
			try {
				for (int i = 0; i < 6; i++) {
					macBytes[i] = (byte) Integer.parseInt(hex[i], 16);
				}
			} catch (Exception e) { LOGGER.error(e.getMessage()); }

			byte[] bytes = new byte[6 + 16 * macBytes.length];
			for (int i = 0; i < 6; i++) {
				bytes[i] = (byte) 0xff; //pad first 6 bytes
			}
			for (int i = 6; i < bytes.length; i += macBytes.length) {
				System.arraycopy(macBytes, 0, bytes, i, macBytes.length); // the rest is the mac bytes repeated 16 times
			}

			//bytes are ready to be sent (magic packet)

			try {

				InetAddress address = InetAddress.getByName(BROADCAST); // local broadcast ip
				DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, 2100);
				DatagramSocket socket = new DatagramSocket(2100);
				socket.setBroadcast(true); // make sure to broadcast
				socket.send(packet);
				socket.close();
				LOGGER.info("Wake up sent");

			} catch (Exception e) {
				LOGGER.error(e.getMessage());
			}
		}
		else {
			LOGGER.info("No MAC provided, so no magic packet sent");
		}

		if(!DToken.equals("") && !DChannelID.equals("") && !DServerID.equals(""))
			try { // update server ip on startup
				JDA bot = JDABuilder.createDefault(DToken).build();
				bot.awaitReady();

				synchronized (bot) {
					bot.wait(DWait);
				}

				//get most recent ip
				bot.getGuildById(DServerID).getTextChannelById(DChannelID).getHistory().retrievePast(1).map(messages -> messages.get(0)).queue(message -> {

					// construct server file
					ListTag<CompoundTag> lt = new ListTag<>(CompoundTag.class);
					CompoundTag innerCt = new CompoundTag();

					innerCt.putString("name", "Minecraft Server");
					innerCt.putString("icon", "");
					innerCt.putString("ip", message.getContentDisplay());
					lt.add(innerCt);

					CompoundTag ct = new CompoundTag();
					ct.put("servers", lt);

					try {
						NBTUtil.write(ct, System.getProperty("user.dir") + "/servers.dat", false);
						LOGGER.info("Replaced Server");
					} catch (Exception e) {
						LOGGER.info(e.getLocalizedMessage());
					}
				});
				bot.shutdown();
			} catch (Exception ex) { LOGGER.info(ex.getLocalizedMessage()); }
		else
			LOGGER.info("Need Tokens/ID for discord");
	}
}

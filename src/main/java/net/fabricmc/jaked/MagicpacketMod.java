package net.fabricmc.jaked;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.requests.RestAction;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.render.DimensionEffects;
import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.io.SNBTUtil;
import net.querz.nbt.tag.*;
import org.apache.commons.lang3.concurrent.Computable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MagicpacketMod implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger("modid");

	@Override
	public void onInitialize() {
		LOGGER.info("Starting magic packet sender");

		String macStr = "08:60:6E:46:EA:FD";
		byte[] macBytes = new byte[6]; // convert mac to bytes
		String[] hex = macStr.split("(\\:|\\-)");
		try {
			for (int i = 0; i < 6; i++) {
				macBytes[i] = (byte) Integer.parseInt(hex[i], 16);
			}
		} catch (Exception e) {}

		byte[] bytes = new byte[6 + 16 * macBytes.length];
		for (int i = 0; i < 6; i++) {
			bytes[i] = (byte) 0xff; //pad first 6 bytes
		}
		for (int i = 6; i < bytes.length; i += macBytes.length) {
			System.arraycopy(macBytes, 0, bytes, i, macBytes.length); // the rest is the mac bytes repeated 16 times
		}

		//bytes are ready to be sent (magic packet)

		try {

			InetAddress address = InetAddress.getByName("255.255.255.255"); // local broadcast ip
			DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, 2100);
			DatagramSocket socket = new DatagramSocket(2100);
			socket.setBroadcast(true); // make sure to broadcast
			socket.send(packet);
			socket.close();
			LOGGER.info("Wake up sent");

		} catch (Exception e) {LOGGER.error(e.getMessage());}

		try { // update server ip on startup
			JDA bot = JDABuilder.createDefault("OTQyMzI3NDU0NzUwNjA5NDA5.Ygi41A.5Sav4_M9pazxBUnYbIh6EK0Cun0").build();
			bot.awaitReady();

			String ip;
			//get most recent ip
			bot.getGuildById("941402252810260522").getTextChannelById("941402253254877217").getHistory().retrievePast(1).map(messages -> messages.get(0)).queue(message -> {

				// construct server file
				ListTag lt = new ListTag(CompoundTag.class);
				CompoundTag innerCt = new CompoundTag();

				innerCt.putString("name", "Dorm Server!");
				innerCt.putString("icon", "");
				innerCt.putString("ip", message.getContentDisplay());
				lt.add(innerCt);

				CompoundTag ct = new CompoundTag();
				ct.put("servers", lt);

				NamedTag nt = new NamedTag("", ct);


				try {
					NBTUtil.write(ct, System.getProperty("user.dir")+"/servers.dat", false);
					LOGGER.info("Replaced Server");
				} catch (Exception e) {e.printStackTrace();}
			});
			bot.shutdown();
		} catch (Exception ex) {
			ex.printStackTrace();
		};
	}
}

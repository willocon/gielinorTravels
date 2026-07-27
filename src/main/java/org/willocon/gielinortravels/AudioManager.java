package org.willocon.gielinortravels;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.audio.AudioPlayer;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.*;


@Slf4j
@Singleton
public class AudioManager
{
	@Inject
	private AudioPlayer audioPlayer;

	@Inject
	private GielinorTravelsConfig config;

	private float gain = 0f;

	@SneakyThrows
	public synchronized void playSound()
	{
		setVolume(config.audioVolume());

		try
		{
			InputStream audioStream = getClass().getResourceAsStream("/sounds/complete.wav");
			if (audioStream == null)
			{
				log.error("Audio file not found.");
				return;
			}
			audioPlayer.play(audioStream, gain);
		}
		catch (IOException e)
		{
			log.error("Error playing sound: ", e);
		}
	}

	public void setVolume(int volume)
	{
		float volumeFloat = volume/100f;
		volumeFloat = Math.max(volumeFloat, 0f);
		volumeFloat = Math.min(volumeFloat, 2f);
		gain = (20f * (float) Math.log10(volumeFloat));
	}
}

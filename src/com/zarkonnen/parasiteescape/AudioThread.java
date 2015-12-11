package com.zarkonnen.parasiteescape;

import java.util.ArrayList;
import java.util.LinkedList;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;

public class AudioThread implements Runnable {
	public static final class AudioRequest {
		final int start;
		final int end;
		final int samps;
		final float rate;
		final float vol;

		public AudioRequest(int start, int end, int samps, float rate, float vol) {
			this.start = start;
			this.end = end;
			this.samps = samps;
			this.rate = rate;
			this.vol = vol;
		}
	}
	
	private final LinkedList<AudioRequest> requests = new LinkedList<AudioRequest>();
	
	public void addRequest(AudioRequest r) {
		synchronized (requests) {
			requests.add(r);
		}
	}
	
	@Override
	public void run() {
		while (true) {
			boolean arIsEmpty = false;
			synchronized (requests) {
				arIsEmpty = requests.isEmpty();
			}
			if (arIsEmpty) {
				try {
					Thread.sleep(10);
				} catch (Exception e) {}
			}
			AudioRequest ar = null;
			synchronized (requests) {
				ar = requests.pollFirst();
			}
			if (ar != null) {
				play(ar);
			}
		}
	}
	
	private static class ClipAndSample {
		public Clip clip;
		public byte[] sample;

		public ClipAndSample(Clip clip) {
			this.clip = clip;
		}
	}
	
	ArrayList<ClipAndSample> clips = new ArrayList<ClipAndSample>();
	ArrayList<byte[]> samples = new ArrayList<byte[]>();
	
	private void play(AudioRequest ar) {
		try {
			AudioFormat audioFormat = new AudioFormat(ar.rate, 8, 1, true, true);
			if (clips.isEmpty()) {
				DataLine.Info info = new DataLine.Info(Clip.class, audioFormat);
				for (int i = 0; i < 10; i++) {
					clips.add(new ClipAndSample((Clip) AudioSystem.getLine(info)));
				}
			}
			ClipAndSample snd = null;
			for (ClipAndSample cas : clips) {
				if (!cas.clip.isActive()) {
					cas.clip.close();
					cas.sample = null;
					snd = cas;
				}
			}
			if (snd == null) {
				DataLine.Info info = new DataLine.Info(Clip.class, audioFormat);
				snd = new ClipAndSample((Clip) AudioSystem.getLine(info));
				clips.add(snd);
			}
			
			lp: for (byte[] sample : samples) {
				if (sample.length != ar.samps) { continue; }
				for (ClipAndSample cas : clips) {
					if (cas.sample == sample) {
						continue lp;
					}
				}
				snd.sample = sample;
				break;
			}
			
			if (snd.sample == null) {
				snd.sample = new byte[ar.samps];
				samples.add(snd.sample);
			}
			
			int i = 0;
			lp:
			while (true) {
				for (int x = ar.start; x < ar.end; x++) {
					i++;
					if (i == ar.samps) {
						break lp;
					}
					snd.sample[i] = (byte) (ParasiteEscape.levelMaps.charAt(x) * ar.vol / 4);
					if (i > ar.samps - 200) {
						snd.sample[i] = (byte) (snd.sample[i] * (ar.samps - i) / 200);
					}
				}
			}
			snd.clip.open(audioFormat, snd.sample, 0, snd.sample.length);
			snd.clip.start();
		} catch (Exception e) {
		}
	}
}

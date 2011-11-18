/**
 * 
 */
package plugins.tprovoost.Microscopy.MicroscopeLive;

import icy.system.thread.ThreadUtil;
import icy.type.DataType;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.LivePainter;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.LiveSequence;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicroscopeCore;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicroscopeImage;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.Tools.ImageGetter;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.Tools.StageMover;


/**
 * Class to handle Live Sequences.
 * Singleton Pattern
 * @author Thomas Provoost
 */
public class LiveModeFrame extends LiveSequence {

	/** Actual Frame thread. */
	private LiveModeThread _thread;
	/** Reference to core (MicroManager). */
	private MicroscopeCore core;
	/** Video Renderer. */
	private MicroscopeImage video;
	/** Refresh Rate calculated according to camera frequency */
	private int refresh_rate;
	/**
	 * LiveModeFrame constructor.
	 * @param buffer : a correct IcyBufferedImage to set up the LiveModeFrame correctly.
	 * @param mCore : Reference to actual core.
	 */
	public LiveModeFrame(MicroscopeImage buffer) {
		super("Live Mode Video",buffer);
		core = MicroscopeCore.getCore();
		_thread = new LiveModeThread();
		video = buffer;
		refresh_rate=90;
		addPainter(new LivePainter(this));
	}
		
	void resolutionChanged() {
		video = new MicroscopeImage((int) core.getImageWidth(), (int) core.getImageHeight(), 1, DataType.USHORT);
		setImage(0, 0, video);
	}
	
	/**
	 * Starts the thread.
	 */
	public void startLive() {
		if (_thread == null) {
			_thread = new LiveModeThread();
		}
		if (_thread.isAlive()) {
			System.out.println("Thread already started");
			return;
		}
		_thread.start();
	}
	
	public void pauseLive() {
		if (_thread != null) {
			// pause the thread
			_thread.please_wait = true;
		}
	}
	
	public void resumeLive() {
		if (_thread != null) {
			// Resume the thread
			_thread.please_wait = false;
		}
	}
	
	/**
	 * Stops the thread.
	 */
	public void stopLive() {
		if (_thread != null) {
			_thread.please_wait = true;
			_thread.interrupt();
			while(!_thread.isInterrupted()) {
				_thread.interrupt();
			}
			try {
				_thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			_thread=null;
		}
	}
	
	public void setRefreshRate(int cfrequency) {
		refresh_rate=(int)((1.0/cfrequency)*1000);
	}
			
	/**
	 * Thread for the live sequence
	 * @author Thomas Provoost
	 */
	private class LiveModeThread extends Thread {
		
		/** Used to pause the thread */
		boolean please_wait=false;
		
		@Override
		public void run() {
			super.run();
			while (true) {
				try {
					while (please_wait) {
						try {
							sleep(200);
						} catch (InterruptedException e) {
							throw e;
						}
					}
					video = (MicroscopeImage)getFirstImage();
					if (video.getWidth() != (int) core.getImageWidth() || video.getHeight() != (int) core.getImageHeight()) {
						if (core.getImageWidth() <= 0 || core.getImageHeight() <= 0)
							throw new InterruptedException();
						resolutionChanged();
					}
					if (!updating) {
						ThreadUtil.bgRun(new Runnable() {
							
							@Override
							public void run() {
								setUpdating(true);
								short [] table = ImageGetter.getImageFromLiveToShort(core);
								if (table != null) {
									try {
										video.setDataXYAsShort(0,table);
										video.setXYZ(StageMover.getXYZ());
										video.setExposure(core.getExposure());
									} catch (Exception e) {
										// MessageDialog.showDialog("Live 3D","In order to use the 3D Live video, please Download Microscope Live 3D Plugin.");
									}
								}
								table = null;
								setUpdating(false);
							}
						});
						try {
							notifyListeners();
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					}
					sleep(refresh_rate);
				}
				catch (InterruptedException e1) {
					return;
				}
			}
		}		
	}
	
	@Override
	public String toString() {
		return super.toString()+"LiveModeFrame";
	}
}
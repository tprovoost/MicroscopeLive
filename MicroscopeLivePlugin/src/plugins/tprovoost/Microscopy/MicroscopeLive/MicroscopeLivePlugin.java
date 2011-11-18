package plugins.tprovoost.Microscopy.MicroscopeLive;

import icy.gui.frame.progress.AnnounceFrame;
import icy.main.Icy;
import icy.sequence.Sequence;
import icy.sequence.SequenceAdapter;

import org.micromanager.utils.StateItem;

import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicroscopeCore;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicroscopeImage;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicroscopePlugin;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.Tools.ImageGetter;

public class MicroscopeLivePlugin extends MicroscopePlugin {

	private LiveModeFrame _livemode;

	@Override
	public void start() {
		// Acquisition of the first image to set up the settings
		MicroscopeImage imgFirst = ImageGetter.snapImage(MicroscopeCore.getCore());

		// Tests if null. In this case, displays a message error and quits the plugin.
		if (imgFirst == null) {
			new AnnounceFrame("Error with acquisition of the first image.");
			return;
		}

		// Creation of the video frame with this first image.
		_livemode = new LiveModeFrame(imgFirst);

		// Tests if frame null. Displays a message error if true and quits the plugin.
		if (_livemode == null) {
			new AnnounceFrame("Error while capturing image.");
			return;
		}

		Icy.addSequence(_livemode);
		// add the video to the main panel

		// ask for a continuous acquisition : instead of snapping and getting images,
		// images are continuously snapped and the last one is used.
		mainGui.continuousAcquisitionNeeded(this);

		// starts the video
		_livemode.startLive();

		// add the plugin to the GUI
		mainGui.addPlugin(this);

		// sets listener on the frame in order to remove this plugin  
		// from the GUI when the frame is closed
		_livemode.addListener(new SequenceAdapter() {
			@Override
			public void sequenceClosed(Sequence sequence) {
				super.sequenceClosed(sequence);
				mainGui.continuousAcquisitionReleased(MicroscopeLivePlugin.this);
				mainGui.removePlugin(MicroscopeLivePlugin.this);
			}
		});
	}

	@Override
	public void notifyConfigAboutToChange(StateItem item) {
		// Pause the video when config is about to change
		_livemode.pauseLive();
		mainGui.continuousAcquisitionReleased(MicroscopeLivePlugin.this);
	}

	@Override
	public void notifyConfigChanged(StateItem item) throws Exception {
		// Resume the video when config is about to change
		_livemode.resumeLive();
		mainGui.continuousAcquisitionNeeded(MicroscopeLivePlugin.this);
	}

	@Override
	public void MainGUIClosed() {
		if (_livemode != null)
			_livemode.stopLive();
	}
}

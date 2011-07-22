package com.foglyn.ui;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

public class FoglynImages {

	private static ImageRegistry imageRegistry = new ImageRegistry();

	private static final URL baseURL = FoglynUIPlugin.getDefault().getBundle().getEntry("/icons/");

	public static final ImageDescriptor OVERLAY_BUG = create("bug.gif");

	public static final ImageDescriptor OVERLAY_FEATURE = create("feature.gif");

    public static final ImageDescriptor OVERLAY_INQUIRY = create("inquiry.gif");

    public static final ImageDescriptor OVERLAY_SCHEDULE_ITEM = create("schedule.gif");

    public static final ImageDescriptor UP_DOWN_SMALL = create("up-down-small.gif");
    
    // FIXME: use icon from Mylyn (this is copy)
    public static final ImageDescriptor COLLAPSE_ALL_SMALL = create("collapseall-small.png");

    public static final ImageDescriptor EXPAND_ALL_SMALL = create("expandall-small.png");

    public static final ImageDescriptor CALENDAR = create("calendar.gif");
    
    public static final ImageDescriptor FOGBUGZ_REPOSITORY = create("fogbugz-banner-repository.png");

    /**
     * @deprecated Use {@link #FOGBUGZ_REPOSITORY} instead
     */
    public static final ImageDescriptor FOGBUGZ_REPOSITORY_SETTINGS = FOGBUGZ_REPOSITORY;
    
	private static ImageDescriptor create(String name) {
		try {
			return ImageDescriptor.createFromURL(makeIconFileURL(name));
		} catch (MalformedURLException e) {
			return ImageDescriptor.getMissingImageDescriptor();
		}
	}

	private static URL makeIconFileURL(String name) throws MalformedURLException {
		if (baseURL == null) {
			throw new MalformedURLException();
		}

		return new URL(baseURL, name);
	}

	private static ImageRegistry getImageRegistry() {
		return imageRegistry;
	}

	/**
	 * Lazily initializes image map.
	 */
	public static Image getImage(ImageDescriptor imageDescriptor) {
		ImageRegistry imageRegistry = getImageRegistry();

		Image image = imageRegistry.get("" + imageDescriptor.hashCode());
		if (image == null) {
			image = imageDescriptor.createImage();
			imageRegistry.put("" + imageDescriptor.hashCode(), image);
		}
		return image;
	}
}

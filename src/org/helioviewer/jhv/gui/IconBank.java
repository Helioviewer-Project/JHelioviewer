package org.helioviewer.jhv.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.net.URL;

import javax.swing.ImageIcon;

/**
 * This class provides access to all images, icons and cursors which are used by
 * the program.
 */
public class IconBank
{
    /**
     * The enum has all the icons, you supply these enums to the getIcon method.
     */
    public static enum JHVIcon
    {
        // The formatter will not merge together multiple lines, if at least one
        // empty line is inserted in between:
        PROPERTIES("properties16.gif"),
        BLANK("blank_square.gif"),
        ADD("edit_add.png"),
        ADD_NEW("add_128x128.png"),
        DOWNLOAD_NEW("download_128x128.png"),
        INFO_NEW("info_128x128.png"),
        DOWNLOAD("download_dm.png"),

        // MOVIE CONTROLS
        PLAY("play_dm.png"),
        PAUSE("agt_pause-queue.png"),
        BACK("agt_back.png"),
        FORWARD("agt_forward.png"),
        PLAY_NEW("TriangleRight_128x128.png"),
        PAUSE_NEW("Pause_128x128.png"),
        BACKWARD_NEW("Backward_128x128.png"),
        FORWARD_NEW("Forward_128x128.png"),
        UP_NEW("TriangleUp_128x128.png"),
        DOWN_NEW("TriangleDown_128x128.png"),
        
        // ZOOMING
        ZOOM_IN("zoomIn24.png"), ZOOM_IN_SMALL("zoomIn24small.png"),
        ZOOM_FIT("zoomFit24.png"), ZOOM_FIT_SMALL("zoomFit24small.png"),
        ZOOM_OUT("zoomOut24.png"), ZOOM_OUT_SMALL("zoomOut24small.png"),
        ZOOM_1TO1("zoom1to124.png"), ZOOM_1TO1_SMALL("zoom1to124small.png"),
        NEW_ZOOM_IN("zoom_in_128x128.png"),
        NEW_ZOOM_OUT("zoom_out_128x128.png"),
        NEW_ZOOM_1TO1("zoom_1_to_1_128x128.png"),
        NEW_ZOOM_FIT("zoom_fit_128x128.png"),
        NEW_ZOOMBOX("zoombox_128x128.png"),
        
        // ARROWS
        LEFT("arrow_left.gif"),
        RIGHT("arrow_right.gif"),
        UP("1uparrow1.png"),
        DOWN("1downarrow1.png"), 
        RIGHT2("arrow.plain.right.gif"),
        DOWN2("arrow.plain.down.gif"),
        NEW_DOWN("arrow_down_128x128.png"),
        NEW_RIGHT("arrow_right_128x128.png"),
		SIMPLE_ARROW_RIGHT("Arrow-Right.png"),
		SIMPLE_ARROW_LEFT("Arrow-Left.png"),
		SIMPLE_DOUBLEARROW_RIGHT("DoubleArrow-Right.png"),
		SIMPLE_DOUBLEARROW_LEFT("DoubleArrow-Left.png"),
        
        // MOUSE POINTERS
        OPEN_HAND("OpenedHand.gif"),
        CLOSED_HAND("ClosedHand.gif"),
        PAN("pan24x24.png"),
        PAN_SELECTED("pan_selected24x24.png"),
        NEW_PAN("pan_arrow_128x128.png"),
        SELECT("select24x24.png"),
        SELECT_SELECTED("select_selected24x24.png"),
        FOCUS("crosshairs24x24.png"),
        FOCUS_SELECTED("crosshairs_checked24x24.png"),

        // MISC ICONS
        VISIBLE("visible_128x128.png"),
        HIDDEN("invisible_128x128.png"),
		REMOVE_LAYER("button_cancel.png"),
		INFO("info.png"),
		REMOVE_NEW("Cancel_128x128.png"),
		CHECK("button_ok.png"),
		EX("button_cancel.png"),
		RUBBERBAND("rubberband.gif"),
		NOIMAGE("NoImageLoaded_256x256.png"),
		WARNING("Warning_128x128.png"),
		CONNECTED("connected_dm.png"),
		DISCONNECTED("not_connected_dm.png"),
		MOVIE_LINK("unlocked.png"),
		MOVIE_UNLINK("locked.png"),
		SPLASH("jhv_splash.png"),
		HVLOGO_SMALL("hvImage_160x160.png"),
		INSTALL4J("install4j.png"),
		RAYGUN_IO("raygun.io.png"),
		SDO_CUT_OUT("sdo_128x128.png"),
		DATE("date.png"),
		CALENDER("Calendar_16x16.png"),
		SHOW_LESS("1uparrow1.png"),
		SHOW_MORE("1downarrow1.png"),
		INVERT("invert_128x128.png"),
		LOADING_BIG("NEW_Loading_256x256.png"), LOADING_SMALL("Loading_219x50.png"),

		// 3D Icons
		MODE_3D("3D_24x24.png"), MODE_3D_SELECTED("3D_selected_24x24.png"),
		MODE_2D("2D_24x24.png"), MODE_2D_SELECTED("2D_selected_24x24.png"),
		RESET("Reset_24x24.png"),
		ROTATE("Rotate_24x24.png"), ROTATE_SELECTED("Rotate_selected_24x24.png"),
		ROTATE_ALL_AXIS("Rotate_both_24x24.png"), ROTATE_ALL_AXIS_SELECTED("Rotate_both_selected_24x24.png"),
		NEW_CAMERA("camera_128x128.png"),
		FULLSCREEN("fullscreen_128x128.png"),
		SETTINGS("settings_128x128.png"),
		CAMERA_MODE_3D("sphere_128x128.png"),
		CAMERA_MODE_2D("circle_128x128.png"),
		NEW_TRACK("track_128x128.png"),
		NEW_ROTATION("Rotation_128x128.png"),
		NEW_ROTATION_Y_AXIS("Rotation_Y_Axis_128x128.png"),
		
		// LAYER ICONS
		SUN_WITH_128x128("sun_with_128x128.png"), SUN_WITHOUT_128x128("sun_without_128x128.png"),
		LAYER_IMAGE_24x24("layer-image_24x24.png"), LAYER_IMAGE_OFF_24x24("layer-image-off_24x24.png"),
		LAYER_IMAGE("layer-image.png"), LAYER_IMAGE_OFF("layer-image-off.png"),
		LAYER_IMAGE_TIME("layer-image-time.png"), LAYER_IMAGE_TIME_OFF("layer-image-time-off.png"),
		LAYER_IMAGE_TIME_MASTER("layer-image-time-master.png"),
		LAYER_MOVIE("layer-movie.png"), LAYER_MOVIE_OFF("layer-movie-off.png"),
		LAYER_MOVIE_TIME("layer-movie-time.png"), LAYER_MOVIE_TIME_OFF("layer-movie-time-off.png"),
		LAYER_MOVIE_TIME_MASTER("layer-movie-time-master.png");

        public final String filename;

        JHVIcon(String _filename)
        {
            filename = _filename;
        }
    };

    /** The location of the image files relative to this folder. */
    private static final String RESOURCE_PATH = "/images/";

    /**
     * Returns the ImageIcon associated with the given enum
     * 
     * @param _icon
     *            enum which represents the image
     * @return the image icon of the given enum
     * */
    public static ImageIcon getIcon(JHVIcon _icon)
    {
        return new ImageIcon(IconBank.class.getResource(RESOURCE_PATH + _icon.filename));
    }

    public static javafx.scene.image.Image getFXImage(JHVIcon _icon)
    {
        return new javafx.scene.image.Image(IconBank.class.getResourceAsStream(RESOURCE_PATH + _icon.filename));
    }
    
    public static ImageIcon getIcon(JHVIcon icon, int width, int height)
    {
        URL imgURL = IconBank.class.getResource(RESOURCE_PATH + icon.filename);
        ImageIcon imageIcon = new ImageIcon(imgURL);
        Image image = imageIcon.getImage();
        image = image.getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING);
        imageIcon.setImage(image);
        return imageIcon;
    	
    }
    
    /**
     * Returns the Image with the given enum.
     * 
     * @param icon
     *            Name of the image which should be loaded
     * @return Image for the given name or null if it fails to load the image.
     * */
    public static BufferedImage getImage(JHVIcon icon)
    {
        ImageIcon imageIcon = getIcon(icon);

        if (imageIcon == null)
            return null;

        Image image = imageIcon.getImage();

        if (image != null && image.getWidth(null) > 0 && image.getHeight(null) > 0)
        {
            BufferedImage bi = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);

            Graphics g = bi.getGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();

            return bi;
        }

        return null;
    }
    
    public static Color getAverageColor(BufferedImage bufImg)
    {
        long sumRed = 0;
        long sumGreen = 0;
        long sumBlue = 0;

        for (int x = 0; x < bufImg.getWidth(); x++)
            for (int y = 0; y < bufImg.getHeight(); y++)
            {
                int argb = bufImg.getRGB(x, y);
                sumRed += (argb>>16) & 0xff;
                sumGreen += (argb>>8) & 0xff;
                sumBlue += (argb>>0) & 0xff;
            }
        
        float divider = bufImg.getWidth() * bufImg.getHeight() * 255f;
        return new Color(sumRed / divider, sumGreen / divider, sumBlue / divider);
    }

    public static BufferedImage stackImages(BufferedImage[] bufImgs, double horizontal, double vertical)
    {
        // exit if no real image data is available
        if (bufImgs.length == 0 || bufImgs[0] == null)
            return null;

        // the first layer is strictly copied
        BufferedImage result = bufImgs[0];

        ColorModel cm = result.getColorModel();
        boolean isAlphaPremultiplied = result.isAlphaPremultiplied();
        WritableRaster raster = result.copyData(null);

        result = new BufferedImage(cm, raster, isAlphaPremultiplied, null);

        int width = result.getWidth();
        int height = result.getHeight();

        Graphics2D gbi = result.createGraphics();
        for (int i = 1; i < bufImgs.length; i++)
        {
            BufferedImage currentImg = bufImgs[i];

            int offsetX = (int) (horizontal * (double) (width - currentImg.getWidth()));
            int offsetY = (int) (vertical * (double) (height - currentImg.getHeight()));
            gbi.drawImage(currentImg, null, offsetX, offsetY);
        }

        return result;
    }
}

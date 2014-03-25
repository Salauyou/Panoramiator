package ru.salauyou.panoramiator;

import java.util.ArrayList;

public interface I_ImageListReceiver {
	
	/* callback when image list was loaded */
	void receiveImageList(ArrayList<C_Image> imagesReceived, int id);

}

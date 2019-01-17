package com.k12nt.k12netframe.utils.helper;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Path;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.io.InputStream;
import java.util.HashMap;
import java.util.logging.Logger;

public class K12NetImageUrlHelper {
	
	private static HashMap<String, Bitmap>  imageUrlMap = new HashMap<String, Bitmap>();
	
	private static Logger logger = Logger.getLogger(K12NetImageUrlHelper.class.getName());

	public static void setBitmap(String image_path, ImageView image_view, Boolean clipped){
		if(imageUrlMap.containsKey(image_path)){
			image_view.setImageBitmap(imageUrlMap.get(image_path));
		}
		else {
			BitmapLoaderAsync loaderAsync = new BitmapLoaderAsync(image_path, image_view, clipped);
			loaderAsync.execute();
		}
	}
	
	public static Bitmap getBitmapDrawable(String path) {
		return imageUrlMap.get(path);
	}
	
	public static class BitmapLoaderAsync extends AsyncTask<Void, Void, Void> {
		
		String image_path;
		ImageView image_view;
		Boolean clipped;

		public BitmapLoaderAsync(String image_path, ImageView image_view,Boolean clipped) {
			super();
			this.image_path = image_path.replace("https", "http");
			this.image_view = image_view;
			this.clipped = clipped;
		}

		@Override
		public void onPreExecute() {
		//	this.image_view.setImageResource(R.drawable.loading);
		}

		@Override
		protected Void doInBackground(Void... params) {

			loadBitmap();
			
			return null;
		}
		
		

		private synchronized void loadBitmap() {
			//Log.d("inf", "resim indirme basladi");
			if(imageUrlMap.containsKey(image_path) == false){
				
				try {
				//	AsistoHelper.logHeap("img stream basladi");
					InputStream in = new java.net.URL(image_path).openStream();
					
				//	if(in != null){
				//	AsistoHelper.logHeap("img decode basladi");
						Bitmap image_bitmap = BitmapFactory.decodeStream(in);
						in.close();

                    if(image_bitmap == null) {
                        return;
                    }
				//	}
				//	AsistoHelper.logHeap("img decode bitti - " + image_path);
					if(clipped) {
						image_bitmap = GetBitmapClippedCircle(image_bitmap); 
						//image_bitmap = android.graphics.Bitmap.createScaledBitmap(image_bitmap,128,128,true);
					}
					imageUrlMap.put(image_path, image_bitmap);
				//	Log.d("inf",  "resim boyut: " + image_path.getBytes().length);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			//Log.d("inf", "resim indirme bitti");
		}
		
		@Override
		public void onPostExecute(Void unused) {
			
			Bitmap bmp = imageUrlMap.get(image_path);
			image_view.setImageBitmap(bmp);
		//	imageUrlMap.remove(image_path);
			
		}
		
		public Bitmap GetBitmapClippedCircle(Bitmap bitmap) {

		        int width = bitmap.getWidth();
		        int height = bitmap.getHeight();
		        
		        int lenght = Math.min(width, height) / 2;
		        
		        final Bitmap outputBitmap = Bitmap.createBitmap(lenght*2, lenght*2, Config.ARGB_8888);

		        final Path path = new Path();
		        
		        path.addCircle(
		        		  (float)(width / 2)
		                , (float) lenght
		                , (float) lenght
		                , Path.Direction.CCW);

		        final Canvas canvas = new Canvas(outputBitmap);
		        canvas.clipPath(path);
		        canvas.drawBitmap(bitmap, 0, 0, null);
		        bitmap.recycle();
		        return outputBitmap;
		    }
		
	}
	

}

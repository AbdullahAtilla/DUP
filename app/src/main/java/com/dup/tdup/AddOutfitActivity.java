package com.dup.tdup;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.Toast;
import java.io.IOException;


public class AddOutfitActivity extends AppCompatActivity
{
    private ImageView imageView;
    private Button picker_btn;
    private Button insert_btn;
    private SeekBar sensitivity_bar;

    private DatabaseManager database;

    private Bitmap selected_bmp = null;
    private int[] background_color = new int[]{255,255,255};

    private static final int PICK_IMAGE = 100;
    private final String TAG = "A- AddOutfit: ";



    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_outfit);

        imageView = (ImageView) findViewById(R.id.imView_add_outfit);
        picker_btn = (Button) findViewById(R.id.button_searchGallery_add_outfit);
        insert_btn = (Button) findViewById(R.id.button_insert_add_outfit);
        database = new DatabaseManager(this);
        sensitivity_bar = (SeekBar) findViewById(R.id.sensitivity_bar_add_outfit);
        sensitivity_bar.setMax(155);
        sensitivity_bar.setMin(0);

        picker_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(v == picker_btn) //start gallery activity
                {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
                }
            }
        });//end picker_btn onClick

        insert_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                PopupMenu popupMenu = new PopupMenu(AddOutfitActivity.this, insert_btn);
                popupMenu.getMenuInflater().inflate(R.menu.menu_outfit_categories, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
                {
                    @Override
                    public boolean onMenuItemClick(MenuItem item)
                    {
                        Toast.makeText(getApplicationContext(), "please wait . . .",
                                Toast.LENGTH_SHORT);

                        String category="";
                        String title = item.getTitle().toString();
                        if(title.equals("Top")){category = "top";}
                        if(title.equals("Long Wears")){category = "long_wears";}
                        if(title.equals("Trousers")){category = "trousers";}
                        if(title.equals("Shorts and Skirts")){category = "shorts_n_skirts";}

                        Bitmap bmp = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
                        boolean result = database.insertOutfit(category, bmp);
                        if(result)
                        {Toast.makeText(getApplicationContext(), "outfit has added into wardrobe", Toast.LENGTH_SHORT).show();}
                        else
                        {Toast.makeText(getApplicationContext(), "outfit can not be added !!", Toast.LENGTH_SHORT).show();}

                        return false;
                    }//end onMenuItemClick
                });//end setOnMenuItemClickListener

                popupMenu.setGravity(Gravity.CENTER);
                popupMenu.show();
                sensitivity_bar.setVisibility(View.GONE);
            }
        });//end insert_btn onClick

        sensitivity_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                ImageProcessor processor = new ImageProcessor();
                Bitmap processed_bmp = processor.extractOutfit(selected_bmp,
                        progress, background_color);
                imageView.setImageBitmap(processed_bmp);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar){}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar){}
        });//end sensitivity_bar.setOnSeekBarChangeListener
    }//end onCreate



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == PICK_IMAGE)
        {
            switch (requestCode)
            {
                case PICK_IMAGE:
                    Uri selectedImage = data.getData();
                    try
                    {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                        selected_bmp = bitmap.copy(Bitmap.Config.ARGB_8888, true);;
                        imageView.setImageBitmap(bitmap);
                        picker_btn.setVisibility(View.GONE);
                        insert_btn.setVisibility(View.VISIBLE);
                        sensitivity_bar.setVisibility(View.VISIBLE);
                    }
                    catch(IOException e){Log.d(TAG, "IO exception " + e);}
                    break;
            }//end switch
        }//end if statement
    }//End onActivityResult
}//end activity class

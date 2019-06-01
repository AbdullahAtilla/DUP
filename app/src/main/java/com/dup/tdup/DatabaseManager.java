package com.dup.tdup;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;


public class DatabaseManager extends SQLiteOpenHelper
{

    private static final String DATABASE_NAME = "DATADUP";
    private final String[] TABLES = {"OUTFIT","MANNEQUIN","COMBINATION"};

    //Constructor
    public DatabaseManager(Context context)
    {super(context, DATABASE_NAME, null, 1);}


    @Override
    public void onCreate(SQLiteDatabase db)
    {
        //OUTFIT TABLE QUERY
        /*
        * id : outfit id
        * category : category of outfit (lower,upper or full-body)
        * image : bitmap image of outfit
        * */
        final String createOutfitTable =
                "CREATE TABLE IF NOT EXISTS OUTFIT (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "category VARCHAR NOT NULL, image BLOB NOT NULL)";
        //MANNEQUIN TABLE QUERY
        /*
         * id : mannequin id
         * image : bitmap image of mannequin
         * */
        final String createMannequinTable =
                "CREATE TABLE IF NOT EXISTS MANNEQUIN (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "image BLOB NOT NULL)";
        //COMBINATION TABLE QUERY
        /*
         * id : combination id
         * name : name of combination
         * upper_outfit_id : id of the outfit as upper part of combination
         * lower_outfit_id : " "   "    "     "  lower part of combination
         * full_outfit_id :  " "   "    "     "  full-body part of combination
         * */
        final String createCombinationTable =
                "CREATE TABLE IF NOT EXISTS COMBINATION (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name VARCHAR NOT NULL, " + "upper_outfit_id INTEGER, lower_outfit_id INTEGER, " +
                        "full_outfit_id INTEGER)";

        db.execSQL(createOutfitTable);
        db.execSQL(createMannequinTable);
        db.execSQL(createCombinationTable);
    }//end onCreate

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        for(int index = 0; index < TABLES.length; index++)
        {
            String table = TABLES[index];
            db.execSQL("DROP TABLE IF EXISTS " + table);
        }
        onCreate(db);
    }//end onUpgrade


    //This function inserts given outfit into database
    public boolean insertOutfit(String category, Bitmap bmp)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] img_byte = stream.toByteArray();
        bmp.recycle();
        ContentValues values = new ContentValues();
        values.put("category", category);
        values.put("image", img_byte);
        long result = db.insert("OUTFIT", null, values);

        db.close();
        if(result == -1) return false;
        else return true;
    }//end insertOutfit

    public ArrayList<Outfit> getOutfitList()
    {
        ArrayList<Outfit> outfitList = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM OUTFIT", null);
        if(cursor.moveToFirst())
        {
            while(!cursor.isAfterLast())
            {
                int id = cursor.getInt(cursor.getColumnIndex("id"));
                String category = cursor.getString(cursor.getColumnIndex("category"));
                byte[] image = cursor.getBlob(cursor.getColumnIndex("image"));
                Outfit outfit = new Outfit(id, category, image);
                outfitList.add(outfit);

                cursor.moveToNext();
            }
        }
        cursor.close();
        db.close();
        return outfitList;
    }//end getOutfitList



    public Outfit getOutfitById(int id)
    {
        final String query = "SELECT * FROM OUTFIT WHERE id = " + id;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        String category = cursor.getString(cursor.getColumnIndex("category"));
        byte[] image = cursor.getBlob(cursor.getColumnIndex("image"));
        Outfit outfit = new Outfit(id, category, image);

        db.close();
        return outfit;
    }//End getOutfitById

    public int deleteOutfitById(int id)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("OUTFIT", "id =? ", new String[]{id+""});
    }//end deleteOutfitById
}//END CLASS

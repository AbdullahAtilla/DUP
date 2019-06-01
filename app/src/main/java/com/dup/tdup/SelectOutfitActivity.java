package com.dup.tdup;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.List;

public class SelectOutfitActivity extends AppCompatActivity
{
    private List<Outfit> outfitList;
    private DatabaseManager databaseManager;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_outfit);

        databaseManager = new DatabaseManager(this);
        outfitList = databaseManager.getOutfitList();

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view_id);
        RecyclerViewAdapter recyclerViewAdapter = new RecyclerViewAdapter(this, outfitList);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setAdapter(recyclerViewAdapter);
    }//end onCreate
}//end class

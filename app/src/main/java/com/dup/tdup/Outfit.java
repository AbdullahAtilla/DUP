package com.dup.tdup;

public class Outfit
{
    private String category;
    private byte[] image;
    private int id;

    public Outfit(int id, String category, byte[] image)
    {
        this.id = id;
        this.category = category;
        this.image = image;
    }//End constructor

    public String getCategory(){return category;}
    public byte[] getImage(){return image;}
    public int getId(){return id;}
}//end class

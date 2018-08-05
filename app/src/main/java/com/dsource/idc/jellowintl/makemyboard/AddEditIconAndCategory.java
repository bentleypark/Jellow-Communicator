package com.dsource.idc.jellowintl.makemyboard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.dsource.idc.jellowintl.DataBaseHelper;
import com.dsource.idc.jellowintl.Nomenclature;
import com.dsource.idc.jellowintl.R;
import com.dsource.idc.jellowintl.makemyboard.UtilityClasses.BoardDatabase;
import com.dsource.idc.jellowintl.makemyboard.UtilityClasses.IconModel;
import com.dsource.idc.jellowintl.makemyboard.UtilityClasses.ModelManager;
import com.dsource.idc.jellowintl.utility.JellowIcon;
import com.dsource.idc.jellowintl.verbiage_model.JellowVerbiageModel;
import com.dsource.idc.jellowintl.verbiage_model.VerbiageDatabaseHelper;
import com.rey.material.app.Dialog;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import static com.dsource.idc.jellowintl.makemyboard.MyBoards.BOARD_ID;
import static com.dsource.idc.jellowintl.makemyboard.MyBoards.CAMERA_REQUEST;
import static com.dsource.idc.jellowintl.makemyboard.MyBoards.GALLERY_REQUEST;
import static com.dsource.idc.jellowintl.makemyboard.MyBoards.LIBRARY_REQUEST;

public class AddEditIconAndCategory extends AppCompatActivity implements View.OnClickListener {

    private static final int ADD_CATEGORY = 212;
    private static final int ADD_ICON = 210;
    private static final int EDIT_ICON = 211;
    private String boardId;
    private BoardDatabase database;
    private Board currentBoard;
    private IconModel boardModel;
    private ModelManager modelManager;
    private RecyclerView categoryRecycler;
    private RecyclerView iconRecycler;
    private ArrayList<String> categories;
    private CategoryManager categoryManager;
    private LevelSelectorAdapter categoryAdapter;
    private IconSelectorAdapter iconAdapter;
    private int previousSelection = 0;
    private ViewTreeObserver.OnGlobalLayoutListener layoutListener;
    private ArrayList<JellowIcon> iconList;
    private ViewTreeObserver.OnGlobalLayoutListener populationListener;
    private RelativeLayout addCategory,addIcon,editIcon;
    private MyBoards.PhotoIntentResult mPhotoIntentResult;
    private int selectedPosition=0;
    private VerbiageDatabaseHelper verbiageDatbase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_icon_select);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(Html.fromHtml("<font color='#333333'>"+"Select icons"+"</font>"));
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_button_board);
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.action_bar_background));
        verbiageDatbase = new VerbiageDatabaseHelper(this,new DataBaseHelper(this).getWritableDatabase());

        try{
            boardId =getIntent().getExtras().getString(BOARD_ID);
        }
        catch (NullPointerException e)
        {
            Log.d("No board id found", boardId);
        }
        database=new BoardDatabase(this);
        currentBoard=database.getBoardById(boardId);
        boardModel = currentBoard.getBoardIconModel();
        modelManager =new ModelManager(this,boardModel);
        iconList = new ArrayList<>();
        categoryManager = new CategoryManager(boardModel);
        categories = categoryManager.getAllCategories();
        iconList = categoryManager.getAllChildOfCategory(0);
        initViews();
    }

    private void initViews() {

        categoryRecycler = findViewById(R.id.level_select_pane_recycler);
        categoryRecycler.setLayoutManager(new LinearLayoutManager(this));
        prepareLevelSelectPane();
        iconRecycler = findViewById(R.id.icon_select_pane_recycler);
        iconRecycler.setLayoutManager(new GridLayoutManager(this,gridSize()));
        iconAdapter = new IconSelectorAdapter(this,
                categoryManager.getAllChildOfCategory(0),
                IconSelectorAdapter.ADD_EDIT_ICON_MODE);
        iconRecycler.setAdapter(iconAdapter);
        iconAdapter.notifyDataSetChanged();
        /*
            Hiding unwanted views of the layout
         */
        ((TextView)findViewById(R.id.reset_selection)).setText("Skip");
        findViewById(R.id.reset_selection).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AddEditIconAndCategory.this,EditBoard.class);
                intent.putExtra(BOARD_ID,boardId);
                startActivity(intent);
                finish();
            }
        });
        findViewById(R.id.next_step).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                database.updateBoardIntoDatabase(new DataBaseHelper(AddEditIconAndCategory.this).getWritableDatabase(),currentBoard);
                Intent intent = new Intent(AddEditIconAndCategory.this,EditBoard.class);
                intent.putExtra(BOARD_ID,boardId);
                startActivity(intent);
                finish();
            }
        });
        findViewById(R.id.select_deselect_check_box).setVisibility(View.GONE);
        findViewById(R.id.icon_count).setVisibility(View.GONE);
        addCategory = findViewById(R.id.add_category);
        addIcon = findViewById(R.id.add_icon);
        editIcon = findViewById(R.id.edit_icon);
        addCategory.setOnClickListener(this);
        editIcon.setOnClickListener(this);
        addIcon.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        toggleOptionLayout();
        int mode = -1;
        if(v ==addCategory)
            mode = ADD_CATEGORY;
        else if(v==addIcon)
            mode = ADD_ICON;
        else if(v==editIcon)
            mode = EDIT_ICON;
        showCustomDialog(mode);

    }

    /**
     * Creates and fetches the left pane for icon select
     */
    private void prepareLevelSelectPane() {

        categoryAdapter =new LevelSelectorAdapter(this,categories);
        categoryRecycler.setAdapter(categoryAdapter);
        layoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                setSelection(previousSelection, null);
            }
        };
        categoryRecycler.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
        categoryAdapter.setOnItemClickListner(new LevelSelectorAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(final View view, final int position) {
                if(previousSelection!=position) {
                            selectedPosition = position;
                            setSelection(position, view);
                            prepareIconPane(position);
                }
            }
        });

    }

    private void prepareIconPane(int position) {
        iconList = categoryManager.getAllChildOfCategory(position);
        iconAdapter = new IconSelectorAdapter(this,iconList,IconSelectorAdapter.ADD_EDIT_ICON_MODE);
        iconRecycler.setAdapter(iconAdapter);
        iconAdapter.notifyDataSetChanged();
    }

    private ViewTreeObserver.OnGlobalLayoutListener tempListener;

    private void setSelection(final int position, final View view) {
                if(view!=null) {

                    View newSelection=categoryRecycler.getChildAt(position);
                    ((TextView)newSelection.findViewById(R.id.icon_title)).setTextColor(getResources().getColor(R.color.colorIntro));
                    view.setBackgroundColor(getResources().getColor(R.color.colorIntroSelected));
                    View prevSelection=categoryRecycler.getChildAt(previousSelection);
                    prevSelection.setBackgroundColor(getResources().getColor(R.color.colorIntro));
                    ((TextView)prevSelection.findViewById(R.id.icon_title)).setTextColor(getResources().getColor(R.color.level_select_text_color));

                }
                else {
                    View s=categoryRecycler.getChildAt(0);
                    s.setBackgroundColor(getResources().getColor(R.color.colorIntroSelected));
                    ((TextView)s.findViewById(R.id.icon_title)).setTextColor(getResources().getColor(R.color.colorIntro));
                    categoryRecycler.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);

                }

        previousSelection =position;
    }

    private int gridSize() {
        int gridSize=6;
        if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE) {
            gridSize=8;
        }
        else if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_NORMAL) {
            gridSize=6;
        }
        else if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_SMALL) {
            gridSize=4;
        }

        return gridSize;
    }

    private void toggleOptionLayout() {
        if(findViewById(R.id.add_edit_icon_option).getVisibility()==View.VISIBLE)
            findViewById(R.id.add_edit_icon_option).setVisibility(View.GONE);
        else
            findViewById(R.id.add_edit_icon_option).setVisibility(View.VISIBLE);
    }

    private void showCustomDialog(int mode) {
        if(mode!=-1)
        {
            if(mode==ADD_ICON)
            initBoardEditAddDialog(mode,-1,"Icon Name");
            else if(mode==ADD_CATEGORY)
                initBoardEditAddDialog(mode,-1,"Category Name");
            else if(mode==EDIT_ICON)
                Toast.makeText(this,"Not implemented yet",Toast.LENGTH_SHORT).show();

        }
    }

    boolean isVisible=false;
    @SuppressLint("ResourceType")
    private void initBoardEditAddDialog(final int mode, final int pos,String editTextHint) {

        View dialogContainerView = LayoutInflater.from(this).inflate(R.layout.edit_board_dialog, null);
        final Dialog dialogForBoardEditAdd = new Dialog(this,R.style.MyDialogBox);
        dialogForBoardEditAdd.applyStyle(R.style.MyDialogBox);
        dialogForBoardEditAdd.backgroundColor(getResources().getColor(R.color.transparent));

        //List on the dialog.
        final ListView listView=dialogContainerView.findViewById(R.id.camera_list);
        final EditText boardTitleEditText=dialogContainerView.findViewById(R.id.board_name);
        boardTitleEditText.setHint(editTextHint);
        TextView saveBoard=dialogContainerView.findViewById(R.id.save_baord);
        TextView cancelSaveBoard=dialogContainerView.findViewById(R.id.cancel_save_baord);
        ImageView editBoardIconButton=dialogContainerView.findViewById(R.id.edit_board);
        final ImageView IconImage=dialogContainerView.findViewById(R.id.board_icon);
        IconImage.setBackground(getResources().getDrawable(R.drawable.icon_back_grey));
        listView.setVisibility(View.GONE);

        // Glide.with(context).load(url).apply(RequestOptions.circleCropTransform()).into(imageView);
/*

        if(code==EDIT_BOARD)
        {
            byte[] bitmapdata=boardList.get(pos).getBoardIcon();
            Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapdata, 0, bitmapdata.length);
            BoardIcon.setImageBitmap(bitmap);
            boardTitleEditText.setText(boardList.get(pos).boardTitle);
        }
*/

        //The list that will be shown with camera options
        final ArrayList<ListItem> list=new ArrayList<>();
        TypedArray mArray=getResources().obtainTypedArray(R.array.add_photo_option);
        list.add(new ListItem("Camera",mArray.getDrawable(0)));
        list.add(new ListItem("Gallery ",mArray.getDrawable(1)));
        list.add(new ListItem("Library ",mArray.getDrawable(2)));
        SimpleListAdapter adapter=new SimpleListAdapter(this,list);
        listView.setAdapter(adapter);
        setOnPhotoSelectListener(new MyBoards.PhotoIntentResult() {
            @Override
            public void onPhotoIntentResult(Bitmap bitmap, int code) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                Glide.with(AddEditIconAndCategory.this)
                        .asBitmap()
                        .load(stream.toByteArray())
                        .apply(RequestOptions.
                                circleCropTransform()).into(IconImage);

            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                listView.setVisibility(View.GONE);
                if(position==0)
                {
                    if(checkPermissionForCamera()) {
                        CropImage.activity()
                                .setAspectRatio(1,1)
                                .setGuidelines(CropImageView.Guidelines.ON)
                                .setFixAspectRatio(true)
                                .start(AddEditIconAndCategory.this);

                      /*
                     Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                     startActivityForResult(takePictureIntent, CAMERA_REQUEST);
                     */
                    }
                    else
                    {
                        final String [] permissions=new String []{ Manifest.permission.CAMERA};
                        ActivityCompat.requestPermissions(AddEditIconAndCategory.this, permissions, CAMERA_REQUEST);
                    }

                }
                else if(position==1)
                {
                    if(checkPermissionForStorageRead()) {
                        Intent selectFromGalleryIntent = new Intent();
                        selectFromGalleryIntent.setType("image/*");
                        selectFromGalleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(selectFromGalleryIntent, GALLERY_REQUEST);
                    }
                    else {
                        final String [] permissions=new String []{ Manifest.permission.READ_EXTERNAL_STORAGE};
                        ActivityCompat.requestPermissions(AddEditIconAndCategory.this, permissions, GALLERY_REQUEST);

                    }
                }
                else if(position==2)
                {
                    Intent intent = new Intent(AddEditIconAndCategory.this,BoardSearch.class);
                    intent.putExtra(BoardSearch.SEARCH_MODE,BoardSearch.ICON_SEARCH);
                    startActivityForResult(intent,LIBRARY_REQUEST);
                }
            }
        });


        saveBoard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name=boardTitleEditText.getText().toString();
                Bitmap icon=((BitmapDrawable)IconImage.getDrawable()).getBitmap();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                icon.compress(Bitmap.CompressFormat.PNG, 50, bos);
                byte[] bitmapArray = bos.toByteArray();

                if(mode==EDIT_ICON)
                    startEditIconMode();
                else if(mode==ADD_CATEGORY)
                    addNewCategory(name,bitmapArray);
                else if(mode==ADD_ICON)
                    addNewIcon(name,bitmapArray,selectedPosition);
                dialogForBoardEditAdd.dismiss();
            }
        });
        cancelSaveBoard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { dialogForBoardEditAdd.dismiss();
            }
        });
        editBoardIconButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isVisible)
                    listView.setVisibility(View.GONE);
                else
                    listView.setVisibility(View.VISIBLE);
                isVisible=!isVisible;
            }
        });
        dialogForBoardEditAdd.setContentView(dialogContainerView);
        dialogForBoardEditAdd.show();

    }

    private void addNewCategory(String name, byte[] bitmapArray) {
        int id  = (int)Calendar.getInstance().getTime().getTime();
        JellowIcon icon = new JellowIcon(name,bitmapArray,-1,-1,id);
        boardModel.addChild(icon);
        categoryManager = new CategoryManager(boardModel);
        categories = categoryManager.getAllCategories();
        verbiageDatbase.addNewVerbiage( Nomenclature.getIconName(icon,this),new JellowVerbiageModel(name));
        currentBoard.setBoardIconModel(boardModel);

      //  database.updateBoardIntoDatabase(new DataBaseHelper(this).getWritableDatabase(),currentBoard);
        prepareLevelSelectPane();
    }

    private void addNewIcon(String name, byte[] bitmapArray, int selectedPosition) {
        int id  = (int)Calendar.getInstance().getTime().getTime();
        JellowIcon icon = new JellowIcon(name,bitmapArray,-1,-1,id);
        boardModel.getChildren().get(selectedPosition).addChild(icon);
        categoryManager = new CategoryManager(boardModel);
        prepareIconPane(selectedPosition);
        currentBoard.setBoardIconModel(boardModel);
        verbiageDatbase.addNewVerbiage(Nomenclature.getIconName(icon,this),new JellowVerbiageModel(name));
        //database.updateBoardIntoDatabase(new DataBaseHelper(this).getWritableDatabase(),currentBoard);
    }

    private void startEditIconMode() {
        Toast.makeText(this,"Yet to implement", Toast.LENGTH_SHORT).show();
    }

    private void setOnPhotoSelectListener(MyBoards.PhotoIntentResult mPhotoIntentResult) {
        this.mPhotoIntentResult=mPhotoIntentResult;
    }

    private boolean checkPermissionForStorageRead() {
        boolean okay=true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                okay = false;
            }
        }

        return okay;
    }

    private boolean checkPermissionForCamera() {
        boolean okay=true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                okay = false;
            }
        }

        return okay;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


        if(requestCode==CAMERA_REQUEST)
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST);
            } else {

                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }

        if(requestCode==GALLERY_REQUEST)
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Gallery Permissions granted
                Intent selectFromGalleryIntent = new Intent();
                selectFromGalleryIntent.setType("image/*");
                selectFromGalleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(selectFromGalleryIntent, GALLERY_REQUEST);
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Bitmap bitmap=null;
        if(resultCode==RESULT_OK) {
            if (requestCode == CAMERA_REQUEST) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    bitmap = (Bitmap) extras.get("data");
                }

            } else if (requestCode == GALLERY_REQUEST) {
                Uri uri = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    mPhotoIntentResult.onPhotoIntentResult(bitmap,requestCode);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else if (requestCode == LIBRARY_REQUEST) {

                byte[] resultImage = data.getByteArrayExtra(getString(R.string.search_result));
                bitmap = BitmapFactory.decodeByteArray(resultImage, 0, resultImage.length);
                mPhotoIntentResult.onPhotoIntentResult(bitmap,requestCode);
            }

           //

        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                Bitmap bitmap1 = result.getBitmap();
                if(bitmap1!=null)
                mPhotoIntentResult.onPhotoIntentResult(result.getBitmap(), requestCode);
                else {
                    try {
                        bitmap1 = MediaStore.Images.Media.getBitmap(getContentResolver(), resultUri);
                        mPhotoIntentResult.onPhotoIntentResult(bitmap1,requestCode);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }

    }

    private void startCrop() {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.add_edit_icon_screen_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.add_edit_icons:
                toggleOptionLayout();
                break;
            case android.R.id.home: finish(); break;
            case R.id.delete_icons: Toast.makeText(this,"Still working on it",Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }


    private class CategoryManager
    {
        private IconModel model;


        private CategoryManager(IconModel model) {
            this.model = model;
        }

        private ArrayList<String> getAllCategories()
        {
            ArrayList<String> list = new ArrayList<>();
            for(int i=0;i<model.getChildren().size();i++)
                list.add(model.getChildren().get(i).getIcon().IconTitle);
            return list;
        }

        private ArrayList<JellowIcon> getAllChildOfCategory(int categoryIndex)
        {
            ArrayList<JellowIcon> iconList  = new ArrayList<>();
            IconModel thisCategory =  model.getChildren().get(categoryIndex);
            //LevelTwo Icons
            ArrayList<IconModel> list = new ArrayList<>(thisCategory.getChildren());

            //Fetching Level three icons of the category.
            for(int i=0;i<thisCategory.getChildren().size();i++)
                list.addAll(thisCategory.getChildren().get(i).getChildren());
            for(int i = 0;i<list.size();i++)
                iconList.add(list.get(i).getIcon());

            return iconList;
        }

    }

}
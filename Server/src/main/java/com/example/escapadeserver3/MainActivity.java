package com.example.escapadeserver3;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileUriExposedException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.escapadeserver3.Adapter.ListingAdapter;
import com.example.escapadeserver3.Model.Category;
import com.example.escapadeserver3.Retrofit.IEscapadeApi;
import com.example.escapadeserver3.Utils.Common;
import com.example.escapadeserver3.Utils.ProgressRequestBody;
import com.example.escapadeserver3.Utils.UploadCallBack;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.ipaulpro.afilechooser.utils.FileUtils;

import java.io.File;
import java.util.List;
import java.util.UUID;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Multipart;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, UploadCallBack {

    private static final int REQUEST_PERMISSION_CODE = 1111;
    private static final int PICK_FILE_REQUEST = 2222;

    RecyclerView recycler_listing;
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    IEscapadeApi mService;

    EditText edt_name;
    ImageView img_browser;
    Uri selected_uri=null;
    String uploaded_img_path="";

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case REQUEST_PERMISSION_CODE:
            {
                if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(this, "permission granted",Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this,"permission denied",Toast.LENGTH_SHORT).show();
            }
            break;
            default:
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED);
        ActivityCompat.requestPermissions(this,new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE

        },REQUEST_PERMISSION_CODE);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddCategoryDialog();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //view
        recycler_listing = (RecyclerView)findViewById(R.id.recycler_listing);
        recycler_listing.setLayoutManager(new GridLayoutManager(this,2));
        recycler_listing.setHasFixedSize(true);

        mService = Common.getApi();

        getListing();

        updateTokenToServer();


    }

    private void updateTokenToServer() {
      FirebaseInstanceId.getInstance().getInstanceId()
              .addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
                  @Override
                  public void onSuccess(InstanceIdResult instanceIdResult) {
                      mService.updateToken("Server_App_01", instanceIdResult.getToken(),"1")
                              .enqueue(new Callback<String>() {
                                  @Override
                                  public void onResponse(Call<String> call, Response<String> response) {
                                      Log.d("Debug", response.body());
                                  }

                                  @Override
                                  public void onFailure(Call<String> call, Throwable t) {
                                      Log.d("Debug", t.getMessage());
                                  }
                              });
                  }
              })
              .addOnFailureListener(new OnFailureListener() {
                  @Override
                  public void onFailure(@NonNull Exception e) {
                      Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                  }
              });
    }

    private void showAddCategoryDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Category");

        View view = LayoutInflater.from(this).inflate(R.layout.add_category_layout,null);

        edt_name = (EditText)view.findViewById(R.id.edt_name);
        img_browser = (ImageView)view.findViewById(R.id.img_browser);

        img_browser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivityForResult(Intent.createChooser(FileUtils.createGetContentIntent(),"Select a file"),PICK_FILE_REQUEST);

            }
        });



        builder.setView(view);
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                uploaded_img_path = "";
                selected_uri=null;
            }
        }).setPositiveButton("ADD", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (edt_name.getText().toString().isEmpty())
                {
                    Toast.makeText(MainActivity.this, "PLEASE ENTER NAME OF CATEGORY", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (uploaded_img_path.isEmpty())
                {
                    Toast.makeText(MainActivity.this, "PLEASE SELECT IMAGE", Toast.LENGTH_SHORT).show();
                    return;
                }

                compositeDisposable.add(mService.addNewCategory(edt_name.getText().toString(),uploaded_img_path)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();

                        getListing();

                        uploaded_img_path="";
                        selected_uri=null;
                    }
                }));

            }
        }).show();


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode== Activity.RESULT_OK)
        {
            if (requestCode == PICK_FILE_REQUEST)
            {
                if (data!=null)
                {
                    selected_uri = data.getData();
                    if (selected_uri != null && !selected_uri.getPath().isEmpty())
                    {
                        img_browser.setImageURI(selected_uri);
                        uploadFileToServer();
                    }
                    else Toast.makeText(this,"",Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void uploadFileToServer() {

        if (selected_uri != null)
        {
            File file = FileUtils.getFile(this,selected_uri);
            String fileName = new StringBuilder(UUID.randomUUID().toString())
                    .append(FileUtils.getExtension(file.toString())).toString();

            ProgressRequestBody requestFile = new ProgressRequestBody(file,this);

            final MultipartBody.Part body = MultipartBody.Part.createFormData("uploaded_file",fileName,requestFile);
            new Thread(new Runnable() {
                @Override
                public void run() {

                    mService.uploadCategoryFile(body)
                            .enqueue(new Callback<String>() {
                                @Override
                                public void onResponse(Call<String> call, Response<String> response) {
                                    uploaded_img_path= new StringBuilder(Common.BASE_URL)
                                            .append("server/category/")
                                            .append(response.body().toString())
                                            .toString();

                                    Log.d("imgPath",uploaded_img_path);
                                    
                                }

                                @Override
                                public void onFailure(Call<String> call, Throwable t) {
                                    Toast.makeText(MainActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();

                                }
                            });
                }
            }).start();

        }

    }

    private void getListing() {
        compositeDisposable
                .add(mService.getMenu().observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(new Consumer<List<Category>>() {
            @Override
            public void accept(List<Category> categories) throws Exception {
                displayListingList(categories);
            }
        }));

    }

    private void displayListingList(List<Category> categories) {
        ListingAdapter adapter = new ListingAdapter(this,categories);
        recycler_listing.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        getListing();
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_show_order) {



            startActivity(new Intent(MainActivity.this,ShowOrderActivity.class));

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    @Override
    public void onProgressUpdate(int percentage) {



    }
}

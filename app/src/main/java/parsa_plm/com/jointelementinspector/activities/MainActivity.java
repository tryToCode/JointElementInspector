package parsa_plm.com.jointelementinspector.activities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.Toast;

import com.jointelementinspector.main.R;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.squareup.leakcanary.LeakCanary;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import parsa_plm.com.jointelementinspector.adapters.PagerAdapter;
import parsa_plm.com.jointelementinspector.customLayout.NoSwipeViewPager;
import parsa_plm.com.jointelementinspector.models.ExpandableListHeader;
import parsa_plm.com.jointelementinspector.fragments.OverviewTabFragment;
import parsa_plm.com.jointelementinspector.utils.AppConstants;

public class MainActivity extends AppCompatActivity implements OverviewTabFragment.onFragmentInteractionListener {
    private final String TAG = getClass().getName();
    private ExpandableListHeader headerData;
    // 20161101: make it global
    TabLayout tabLayout;
    private boolean inSpecificFolder = false;
    private String mSpecificFolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 20161023: this code only for test
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(getApplication());
        setContentView(R.layout.menu_toolbar);
        Toolbar menuToolBar = (Toolbar) findViewById(R.id.menu_toolbar);
        if (menuToolBar != null) {
            // hide the tool bar title
            menuToolBar.setTitle("");
            setSupportActionBar(menuToolBar);
            initDrawer(menuToolBar);
        }
        // data transport from open file activity
        Bundle bundle = getIntent().getExtras();
        if (bundle != null)
            headerData = bundle.getParcelable(AppConstants.PARCELABLE);
        // 2016114: extract method
        setUpTab();
        setUpViewPager(tabLayout);
    }
    @Override
    protected void onResume() {
        super.onResume();
        // 20170611: reopen last activity
        SharedPreferences prefs = getSharedPreferences(AppConstants.JOINT_ELEMENT_PREF, MODE_PRIVATE);
        String lastActivity = prefs.getString(AppConstants.LAST_ACTIVITY, null);
        if (lastActivity != null)
            dispatchActivity(lastActivity);
    }
    private void dispatchActivity(String lastActivity) {
        Class<?> activityClass = null;
        try {
            activityClass = Class.forName(lastActivity);
        } catch (ClassNotFoundException ex) {
            Log.i(TAG, "dispatchActivity: " + ex.toString());
        }
        if (activityClass != null)
            startActivity(new Intent(this, activityClass));
    }
    private void initDrawer(Toolbar menuToolBar) {
        PrimaryDrawerItem openFile = new PrimaryDrawerItem()
                .withIcon(R.mipmap.ic_viewfile)
                .withName(R.string.drawer_item_openfile);
        PrimaryDrawerItem openFromServer = new PrimaryDrawerItem()
                .withIcon(R.mipmap.ic_doc)
                .withName(R.string.drawer_item_openFromServer);
        PrimaryDrawerItem save = new PrimaryDrawerItem()
                .withIcon(R.mipmap.ic_save)
                .withName(R.string.drawer_item_save);
        PrimaryDrawerItem saveAs = new PrimaryDrawerItem()
                .withIcon(R.mipmap.ic_saveas)
                .withName(R.string.drawer_item_saveAs);
        PrimaryDrawerItem saveReport = new PrimaryDrawerItem()
                .withIcon(R.mipmap.ic_sendfile)
                .withName(R.string.drawer_item_saveExcelReport);
        PrimaryDrawerItem photo = new PrimaryDrawerItem()
                .withIcon(R.mipmap.ic_camera)
                .withName(R.string.drawer_item_takePhoto);
        SecondaryDrawerItem aboutUs = new SecondaryDrawerItem()
                .withIcon(R.mipmap.ic_aboutus)
                .withName(R.string.drawer_item_aboutUs);
        SecondaryDrawerItem contact = new SecondaryDrawerItem()
                .withIcon(R.mipmap.ic_contact)
                .withName(R.string.drawer_item_contact);
        AccountHeader header = initHeader();

        Drawer result = new DrawerBuilder()
                .withAccountHeader(header)
                .withActivity(this)
                .withToolbar(menuToolBar)
                .withSelectedItem(-1)
                .addDrawerItems(
                        openFile,
                        openFromServer,
                        save,
                        saveAs,
                        saveReport,
                        photo,
                        new DividerDrawerItem(),
                        aboutUs,
                        contact
                ).build();
        setUpDrawerItemClick(result);
    }
    private void setUpDrawerItemClick(Drawer result) {
        result.setOnDrawerItemClickListener((view, position, drawerItem) -> {
            switch (position) {
                case 1:
                    onOpenFileClick();
                    result.closeDrawer();
                    break;
                case 2:
                    Toast.makeText(this, AppConstants.PDF_FROM_SERVER, Toast.LENGTH_LONG).show();
                    result.closeDrawer();
                    break;
                case 3:
                    Toast.makeText(this, AppConstants.SAVE, Toast.LENGTH_LONG).show();
                    result.closeDrawer();
                    break;
                case 4:
                    Toast.makeText(this, AppConstants.SAVE_AS, Toast.LENGTH_LONG).show();
                    result.closeDrawer();
                    break;
                case 5:
                    Toast.makeText(this, AppConstants.SAVE_REPORT, Toast.LENGTH_LONG).show();
                    result.closeDrawer();
                    break;
                case 6:
                    onPrepareCapturePhoto();
                    result.closeDrawer();
                    break;
            }
            return true;
        });
    }
    private void onOpenFileClick() {
        Intent openFileIntent = new Intent(this, OpenFileActivity.class);
        openFileIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityForResult(openFileIntent, AppConstants.REQUEST_CODE);
    }
    private AccountHeader initHeader() {
        return new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.drawerheader)
                .withSelectionListEnabledForSingleProfile(false)
                .addProfiles(
                        new ProfileDrawerItem().withName(AppConstants.PARSA_PLM)
                )
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile) {
                        return false;
                    }
                })
                .build();
    }
    // 20170211: change view pager header text to icon
    private void setUpTab() {
        tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        if (tabLayout != null) {
            tabLayout.addTab(tabLayout.newTab().setText(AppConstants.TITLE_OVERVIEW));
            // 20170113: 3d viewer
            tabLayout.addTab(tabLayout.newTab().setText(AppConstants.TITLE_VISUALVIEWER));
            tabLayout.addTab(tabLayout.newTab().setText(AppConstants.TITLE_DOCUMENT));
            tabLayout.addTab(tabLayout.newTab().setText(AppConstants.TITLE_PHOTOS));
            tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        }
    }
    // 20161031: this one should be used to obtain data
    // 20161101: debug result: get data successful from open file activity, but we still have to pass
    // data to view pager, maybe later check it out
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        File photoFile = null;
        FileOutputStream outputStream = null;
        Bitmap bitmap = null;
        if (resultCode == Activity.RESULT_OK && requestCode == AppConstants.CAMERA_CAPTURE) {
            try {
                photoFile = createImageFile();
                outputStream = new FileOutputStream(photoFile);
                bitmap = (Bitmap) intent.getExtras().get("data");
                if (bitmap != null)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 85, outputStream);
                outputStream.flush();
                outputStream.close();
            } catch (IOException ex) {
                Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            }
            // 20170108: inform user where the photo was stored
            if (inSpecificFolder)
                Toast.makeText(this, AppConstants.PHOTO_STORED_MESSAGE + mSpecificFolder, Toast.LENGTH_LONG).show();
            else
                Toast.makeText(this, AppConstants.PHOTO_STORED_MESSAGE + this.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString(), Toast.LENGTH_LONG).show();
        }
    }
    // test removed argument final TabLayout tabLayout
    private void setUpViewPager(final TabLayout tabLayout) {
        // 20170331: disable swiping of view pager for better usability
        final NoSwipeViewPager viewPager = (NoSwipeViewPager) findViewById(R.id.noSwipePager);
        viewPager.setOffscreenPageLimit(3);
        final PagerAdapter pagerAdapter = new PagerAdapter(getSupportFragmentManager(), 4);
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_context, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return true;
    }
    // 20161215: should check if device has camera and inform user
    private void onPrepareCapturePhoto() {
        if (!this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Toast.makeText(this, AppConstants.NO_CAMERA, Toast.LENGTH_LONG).show();
            return;
        }
        if (!isExternalStorageWritable()) {
            Toast.makeText(this, AppConstants.EXTERNAL_STORAGE, Toast.LENGTH_LONG).show();
            return;
        }
        onCapturePhoto();
    }
    private void onCapturePhoto() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        if (this.headerData == null) {
            adb.setIcon(R.mipmap.ic_attention);
            adb.setTitle("Photograhieren ohne Anzeigen");
            adb.setMessage("Sie haben noch keine XML Datei geöffnet, wenn Sie " +
                    "fortfahren, sind neu gemachte Bilder nicht in diesem Programm anzuzeigen. ");
            adb.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    captureImage();
                }
            });
            adb.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            adb.show();
        } else {
            // capture image and update grid view for result
            // should check if the specific folder exist
            // communication between main activity and tab photo fragment,
            // also between tab document fragment
            String externalDir = this.getExternalFilesDir(null).toString();
            String[] xmlFileDir = headerData.getFileDirectory().split("/");
            String specificFolder = externalDir + File.separator + xmlFileDir[xmlFileDir.length - 1];
            File file = new File(specificFolder);
            if (!file.isDirectory() || !file.exists()) {
                adb.setIcon(R.mipmap.ic_attention);
                adb.setTitle(AppConstants.NO_DIRECTORY);
                adb.setMessage("Der Ordner, in dem Bilder zu speichern sind, existiert nicht. Stellen Sie sicher," +
                        "dass XML Dateien aus Teamcenter korreckt exportiert werden. ");
                adb.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
            }
            mSpecificFolder = specificFolder;
            inSpecificFolder = true;
            captureImage();
        }
    }
    // store images in specific folder if withSpecificFolder is true, argument file could be null
    // 20161215: create image file in createImageFile
    // 20170107: just use intent to capture photos und push data in the file by onActivityResult
    private void captureImage() {
        try {
            Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(captureIntent, AppConstants.CAMERA_CAPTURE);
        } catch (ActivityNotFoundException e) {
            Toast toast = Toast.makeText(this, AppConstants.CAPUTURE_IMAGE_MASSAGE, Toast.LENGTH_SHORT);
            toast.show();
        }
    }
    // 20161220 image now successfully stored in specific folder
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = timeStamp + "_";
        String storageDir = null;
        if (inSpecificFolder)
            storageDir = mSpecificFolder;
        else
            storageDir = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString();
        File f = new File(storageDir);
        return File.createTempFile(imageFileName, ".jpg", f);
    }
    @Override
    public ExpandableListHeader onFragmentCreated() {
        return headerData != null ? headerData : null;
    }
    // check if external storage available for write und inform user
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(R.mipmap.ic_attention)
                .setTitle(AppConstants.EXIST)
                .setMessage(AppConstants.EXIST_MESSAGE)
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.super.onBackPressed();
                    }
                }).create().show();
    }
}

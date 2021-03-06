package parsa_plm.com.jointelementinspector.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import parsa_plm.com.jointelementinspector.base.BaseTabFragment;
import parsa_plm.com.jointelementinspector.models.ExpandableListHeader;

import com.jointelementinspector.main.R;

import parsa_plm.com.jointelementinspector.adapters.DocumentGridAdapter;
import parsa_plm.com.jointelementinspector.utils.AppConstants;
import parsa_plm.com.jointelementinspector.utils.Utility;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DocumentTabFragment extends BaseTabFragment {
    @BindView(R.id.document_recycler_view)
    RecyclerView mGridView;
    private Context mContext;
    // 20170108: add swipe refresh layout
    @BindView(R.id.document_swipeContainer)
    SwipeRefreshLayout mSwipeRefreshLayout;
    private String mDocFilePath;

    public DocumentTabFragment() {
        setArguments(new Bundle());
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View documentView = inflater.inflate(R.layout.tab_fragment_document, container, false);
        setUnBinder(ButterKnife.bind(this, documentView));
        mContext = getContext();
        if (mGridView != null) {
            mGridView.setHasFixedSize(true);
            mGridView.setItemViewCacheSize(30);
            mGridView.setDrawingCacheEnabled(true);
            mGridView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
            int columns = Utility.calculateColumns(mContext);
            GridLayoutManager gl = new GridLayoutManager(mContext, columns);
            mGridView.setLayoutManager(gl);
        }
        if (mSwipeRefreshLayout != null)
            // 20170108: swipe refresh layout
            mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorPrimary));
        return documentView;
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ExpandableListHeader headerData = getHeaderData();
        if (headerData != null) {
            String mDocumentPath = headerData.getFileDirectory();
            List<File> filePath = getPDFFiles(mDocumentPath);
            if (filePath != null)
                setUpDocumentAdapter(filePath, mDocumentPath);
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        Bundle bundle = getArguments();
        if (bundle != null) {
            String filePath = (String) bundle.get(AppConstants.DOC_FILE_PATH);
            if (filePath != null) {
                onOpenFile(filePath);
                // 20170611: release
                bundle.clear();
                if (mDocFilePath != null)
                    mDocFilePath = null;
            }
        }
    }
    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences prefs = this.getActivity().getSharedPreferences(AppConstants.JOINT_ELEMENT_PREF, Context.MODE_PRIVATE);
        boolean pausedOnBackPressed = prefs.getBoolean(AppConstants.PAUSED_ON_BACK_PRESSED, false);
        // 20170614: if image display activity was paused on back pressed, we do not need to store file path
        if (!pausedOnBackPressed) {
            if (mDocFilePath != null){
                getArguments().putString(AppConstants.DOC_FILE_PATH, mDocFilePath);
                SharedPreferences.Editor edit = prefs.edit();
                edit.remove(AppConstants.PAUSED_ON_BACK_PRESSED);
                edit.apply();
            }
        }
    }
    // 20161223: add listener
    private void setUpDocumentAdapter(List<File> documents, String documentPath) {
        DocumentGridAdapter adapter = new DocumentGridAdapter(mContext, documents, (view, position) -> {
            setUpOnClickListener(position, documents, documentPath);
        });
        setSwipeRefresh(adapter, documentPath);
        if (mGridView != null)
            mGridView.setAdapter(adapter);
    }
    // 20170113: use notifyDataSetChanged which update all items by data inserted, removed, bad performance
    // should use more specific method to update items as notifyDItemRangeChanged etc.
    private void setSwipeRefresh(final DocumentGridAdapter adapter, final String documentPath) {
        // 20170108: swipe refresh, with clear and addAll notify works
        if (mSwipeRefreshLayout == null)
            return;
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            // 20170108: hold reference of the old documents
            List<File> refreshPdfs = getPDFFiles(documentPath);
            if (refreshPdfs == null) {
                mSwipeRefreshLayout.setRefreshing(false);
                return;
            }
            mSwipeRefreshLayout.setRefreshing(false);
            int oldDocumentsCount = adapter.getItemCount();
            if (oldDocumentsCount != refreshPdfs.size()) {
                adapter.clear();
                adapter.addAll(refreshPdfs);
                adapter.notifyDataSetChanged();
                int updatedItemCount = refreshPdfs.size() - oldDocumentsCount;
                if (updatedItemCount > 0)
                    Toast.makeText(mContext, updatedItemCount + AppConstants.ITEM_ADDED, Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(mContext, Math.abs(updatedItemCount) + AppConstants.ITEM_REMOVED, Toast.LENGTH_LONG).show();
            }
        });
    }
    // 20170108: should check if the file exists, which has been clicked
    private void setUpOnClickListener(int position, List<File> documents, String documentPath) {
        mDocFilePath = documents.get(position).getAbsolutePath();
        // 20161220: now the correct path
        // mDocFilePath = documentPath + File.separator + file.getName();
        onOpenFile(mDocFilePath);
    }
    private void onOpenFile(String filePath) {
        if (filePath == null)
            return;
        File mDocFile = new File(filePath);
        if (!mDocFile.exists()) {
            Toast.makeText(mContext, " Can not access file " + mDocFile.toString() + " probably been removed.", Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(mDocFile), AppConstants.INTENT_DATA_TYPE);
        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
        if (activities.size() == 0) {
            Toast.makeText(mContext, AppConstants.NO_INSTALLED_PROGRAM, Toast.LENGTH_LONG).show();
            return;
        }
        mContext.startActivity(intent);
    }
    private List<File> getPDFFiles(String documentPath) {
        List<File> pdfFiles = new ArrayList<>();
        File documentDir = null;
        if (documentPath == null || documentPath.isEmpty())
            return null;
        documentDir = new File(documentPath);
        if (!documentDir.isDirectory() || !documentDir.exists()) {
            new AlertDialog.Builder(mContext)
                    .setIcon(R.mipmap.ic_attention)
                    .setTitle(AppConstants.DOC_PATH_INCORRECT)
                    .setMessage(AppConstants.DOC_PATH_FAILED_MESSAGE)
                    .create().show();
            return null;
        }
        File[] files = documentDir.listFiles();
        if (files.length == 0)
            Toast.makeText(mContext, AppConstants.NO_DOCUMENT, Toast.LENGTH_LONG).show();
        for (File f : files) {
            if (f.getName().toLowerCase().endsWith(".pdf"))
                pdfFiles.add(f);
        }
        return pdfFiles;
    }
}

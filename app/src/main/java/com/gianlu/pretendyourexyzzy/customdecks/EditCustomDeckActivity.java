package com.gianlu.pretendyourexyzzy.customdecks;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.FileProvider;
import androidx.viewpager.widget.ViewPager;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.ThisApplication;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.adapters.PagerAdapter;
import com.gianlu.pretendyourexyzzy.customdecks.edit.BlackCardsFragment;
import com.gianlu.pretendyourexyzzy.customdecks.edit.GeneralInfoFragment;
import com.gianlu.pretendyourexyzzy.customdecks.edit.WhiteCardsFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class EditCustomDeckActivity extends ActivityWithDialog {
    private static final String TAG = EditCustomDeckActivity.class.getSimpleName();
    private GeneralInfoFragment generalInfoFragment;
    private ViewPager pager;
    private AbsCardsFragment whiteCardsFragment;
    private AbsCardsFragment blackCardsFragment;

    public static void startActivityNew(@NonNull Context context) {
        context.startActivity(new Intent(context, EditCustomDeckActivity.class)
                .putExtra("title", context.getString(R.string.createCustomDeck)));
    }

    public static void startActivityEdit(@NonNull Context context, @NonNull CustomDecksDatabase.CustomDeck deck) {
        context.startActivity(new Intent(context, EditCustomDeckActivity.class)
                .putExtra("title", deck.name + " - " + context.getString(R.string.editCustomDeck))
                .putExtra("id", deck.id));
    }

    public static void startActivityImport(@NonNull Context context, boolean importDeck, @NonNull File tmpFile) {
        context.startActivity(new Intent(context, EditCustomDeckActivity.class)
                .putExtra("title", importDeck ? context.getString(R.string.importCustomDeck) : context.getString(R.string.recoverCardcastDeck))
                .putExtra("tmpFile", tmpFile));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_custom_deck, menu);
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_view_custom_deck);
        setTitle(getIntent().getStringExtra("title"));
        setSupportActionBar(findViewById(R.id.editViewCustomDeck_toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        Integer id = getIntent().getIntExtra("id", -1);
        if (id == -1) id = null;

        pager = findViewById(R.id.editViewCustomDeck_pager);
        pager.setOffscreenPageLimit(3);
        TabLayout tabs = findViewById(R.id.editViewCustomDeck_tabs);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                pager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (position != 0 && generalInfoFragment != null && !generalInfoFragment.isSaved()) {
                    if (!save()) {
                        pager.removeOnPageChangeListener(this);
                        pager.setCurrentItem(0);
                        Toaster.with(EditCustomDeckActivity.this).message(R.string.completeDeckInfoFirst).show();
                        pager.addOnPageChangeListener(this);
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        generalInfoFragment = GeneralInfoFragment.get(this, id);
        blackCardsFragment = BlackCardsFragment.get(this);
        whiteCardsFragment = WhiteCardsFragment.get(this);

        if (id != null) {
            CustomDecksHandler handler = new CustomDecksHandler(this, id);
            blackCardsFragment.setHandler(handler);
            whiteCardsFragment.setHandler(handler);
        }

        File tmpFile = (File) getIntent().getSerializableExtra("tmpFile");
        if (tmpFile != null) {
            try {
                JSONObject obj = new JSONObject(CommonUtils.readEntirely(new FileInputStream(tmpFile)));
                generalInfoFragment.set(obj.optString("name"), obj.optString("watermark"), obj.optString("description"));
                if (save()) {
                    blackCardsFragment.importCards(this, obj.optJSONArray("calls"));
                    whiteCardsFragment.importCards(this, obj.optJSONArray("responses"));

                    tmpFile.delete();
                    ThisApplication.sendAnalytics(Utils.ACTION_IMPORTED_CUSTOM_DECK);
                }
            } catch (JSONException | IOException ex) {
                Log.e(TAG, "Failed importing deck.", ex);
            }
        }

        pager.setAdapter(new PagerAdapter(getSupportFragmentManager(), generalInfoFragment, blackCardsFragment, whiteCardsFragment));
        tabs.setupWithViewPager(pager);
    }

    private boolean save() {
        if (generalInfoFragment == null) return false;

        if (generalInfoFragment.save(this)) {
            Integer deckId = generalInfoFragment.getDeckId();
            if (deckId == null) return false;

            CustomDecksHandler handler = new CustomDecksHandler(this, deckId);
            if (whiteCardsFragment != null) whiteCardsFragment.setHandler(handler);
            if (blackCardsFragment != null) blackCardsFragment.setHandler(handler);

            supportInvalidateOptionsMenu();
            return true;
        } else {
            return false;
        }
    }

    private void exportCustomDeckJson() {
        Integer deckId;
        if (generalInfoFragment == null || (deckId = generalInfoFragment.getDeckId()) == null)
            return;

        CustomDecksDatabase db = CustomDecksDatabase.get(this);
        CustomDecksDatabase.CustomDeck deck = db.getDeck(deckId);
        if (deck == null)
            return;

        try {
            JSONObject obj = deck.craftPyxJson(db);

            File parent = new File(getCacheDir(), "exportedDecks");
            if (!parent.exists() && !parent.mkdir()) {
                Log.e(TAG, "Failed creating exported decks directory: " + parent);
                return;
            }

            String fileName = getName();
            if (fileName.isEmpty()) fileName = String.valueOf(deckId);

            File file = new File(parent, fileName + ".json");
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(obj.toString().getBytes());
            }

            Uri uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName(), file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/json");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(intent, "Share custom deck..."));
        } catch (JSONException | IOException | IllegalArgumentException ex) {
            Log.e(TAG, "Failed exporting custom deck!", ex);
        }
    }

    @NonNull
    public String getWatermark() {
        return generalInfoFragment == null ? "" : generalInfoFragment.getWatermark();
    }

    @NonNull
    private String getName() {
        return generalInfoFragment == null ? "" : generalInfoFragment.getName();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!getIntent().hasExtra("id"))
            menu.removeItem(R.id.editCustomDeck_delete);

        if (generalInfoFragment == null || !generalInfoFragment.isSaved())
            menu.removeItem(R.id.editCustomDeck_export);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.editCustomDeck_done:
                if (save()) {
                    onBackPressed();
                } else if (pager != null) {
                    pager.setCurrentItem(0);
                }
                return true;
            case R.id.editCustomDeck_export:
                AnalyticsApplication.sendAnalytics(Utils.ACTION_EXPORTED_CUSTOM_DECK);
                exportCustomDeckJson();
                return true;
            case R.id.editCustomDeck_delete:
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
                builder.setTitle(R.string.delete).setMessage(getString(R.string.deleteDeckConfirmation, getName()))
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                            Integer deckId = generalInfoFragment.getDeckId();
                            if (deckId == null) return;

                            ThisApplication.sendAnalytics(Utils.ACTION_DELETED_CUSTOM_DECK);
                            CustomDecksDatabase.get(EditCustomDeckActivity.this).deleteDeckAndCards(deckId, true);
                            onBackPressed();
                        }).setNegativeButton(android.R.string.no, null);

                showDialog(builder);
                return true;
            default:
                return false;
        }
    }
}

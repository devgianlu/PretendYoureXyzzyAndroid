package com.gianlu.pretendyourexyzzy.customdecks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.viewpager.widget.ViewPager;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.adapters.PagerAdapter;
import com.gianlu.pretendyourexyzzy.customdecks.edit.AbsCardsFragment;
import com.gianlu.pretendyourexyzzy.customdecks.edit.BlackCardsFragment;
import com.gianlu.pretendyourexyzzy.customdecks.edit.GeneralInfoFragment;
import com.gianlu.pretendyourexyzzy.customdecks.edit.WhiteCardsFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class EditCustomDeckActivity extends ActivityWithDialog {
    private static final String TAG = EditCustomDeckActivity.class.getSimpleName();
    private GeneralInfoFragment generalInfoFragment;
    private ViewPager pager;
    private AbsCardsFragment whiteCardsFragment;
    private AbsCardsFragment blackCardsFragment;
    private Integer id;

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

        id = getIntent().getIntExtra("id", -1);
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
        blackCardsFragment = BlackCardsFragment.get(this, id);
        whiteCardsFragment = WhiteCardsFragment.get(this, id);

        File tmpFile = (File) getIntent().getSerializableExtra("tmpFile");
        if (tmpFile != null) {
            try {
                JSONObject obj = new JSONObject(CommonUtils.readEntirely(new FileInputStream(tmpFile)));
                generalInfoFragment.set(obj.optString("name"), obj.optString("watermark"), obj.optString("description"));
                if (save()) {
                    blackCardsFragment.importCards(this, obj.optJSONArray("calls"));
                    whiteCardsFragment.importCards(this, obj.optJSONArray("responses"));

                    tmpFile.delete();
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

            if (whiteCardsFragment != null) whiteCardsFragment.setDeckId(deckId);
            if (blackCardsFragment != null) blackCardsFragment.setDeckId(deckId);
            return true;
        } else {
            return false;
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
        if (!getIntent().hasExtra("id")) menu.removeItem(R.id.editCustomDeck_delete);
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
            case R.id.editCustomDeck_delete:
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
                builder.setTitle(R.string.delete).setMessage(getString(R.string.deleteDeckConfirmation, getName()))
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                            if (id == null) return;
                            CustomDecksDatabase.get(EditCustomDeckActivity.this).deleteDeckAndCards(id, true);
                            onBackPressed();
                        }).setNegativeButton(android.R.string.no, null);

                showDialog(builder);
                return true;
            default:
                return false;
        }
    }
}

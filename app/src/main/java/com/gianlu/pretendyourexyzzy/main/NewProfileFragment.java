package com.gianlu.pretendyourexyzzy.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.adapters.OrderedRecyclerViewAdapter;
import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.AchievementProgressView;
import com.gianlu.pretendyourexyzzy.GPGamesHelper;
import com.gianlu.pretendyourexyzzy.NewMainActivity;
import com.gianlu.pretendyourexyzzy.PK;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.adapters.NewCustomDecksAdapter;
import com.gianlu.pretendyourexyzzy.adapters.NewStarredCardsAdapter;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.StatusCodeException;
import com.gianlu.pretendyourexyzzy.api.crcast.CrCastApi;
import com.gianlu.pretendyourexyzzy.api.crcast.CrCastDeck;
import com.gianlu.pretendyourexyzzy.cards.CardSize;
import com.gianlu.pretendyourexyzzy.customdecks.BasicCustomDeck;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase;
import com.gianlu.pretendyourexyzzy.customdecks.NewEditCustomDeckActivity;
import com.gianlu.pretendyourexyzzy.databinding.FragmentNewProfileBinding;
import com.gianlu.pretendyourexyzzy.databinding.ItemFriendBinding;
import com.gianlu.pretendyourexyzzy.dialogs.CrCastLoginDialog;
import com.gianlu.pretendyourexyzzy.dialogs.NewChatDialog;
import com.gianlu.pretendyourexyzzy.dialogs.NewUserInfoDialog;
import com.gianlu.pretendyourexyzzy.dialogs.OverloadedSubDialog;
import com.gianlu.pretendyourexyzzy.overloaded.ChatsListActivity;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;
import com.gianlu.pretendyourexyzzy.starred.StarredCardsDatabase;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.games.achievement.Achievement;
import com.google.android.gms.games.event.Event;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.callback.SuccessCallback;
import xyz.gianlu.pyxoverloaded.model.FriendStatus;
import xyz.gianlu.pyxoverloaded.model.UserData;

import static com.gianlu.pretendyourexyzzy.GPGamesHelper.setEventCount;

public class NewProfileFragment extends NewMainActivity.ChildFragment implements OverloadedApi.EventListener, CrCastLoginDialog.LoginListener {
    private static final String TAG = NewProfileFragment.class.getSimpleName();
    private static final int RC_IMPORT_JSON = 420;
    private FragmentNewProfileBinding binding;
    private RegisteredPyx pyx;
    private FriendsAdapter friendsAdapter;
    private NewCustomDecksAdapter customDecksAdapter;

    @NonNull
    public static NewProfileFragment get() {
        return new NewProfileFragment();
    }

    @NonNull
    private static List<Achievement> getAchievementsInProgress(@NonNull Iterable<Achievement> iter) {
        List<Achievement> list = new ArrayList<>(10);
        Map<String, Achievement> map = new HashMap<>(3);
        for (Achievement ach : iter) {
            if (ach.getType() != Achievement.TYPE_INCREMENTAL || ach.getState() != Achievement.STATE_REVEALED)
                continue;

            if (ach.getTotalSteps() == ach.getCurrentSteps())
                continue;

            int index;
            if ((index = CommonUtils.indexOf(GPGamesHelper.ACHS_WIN_ROUNDS, ach.getAchievementId())) != -1) {
                Achievement prev = map.get("winRounds");
                if (prev == null || index < CommonUtils.indexOf(GPGamesHelper.ACHS_WIN_ROUNDS, prev))
                    map.put("winRounds", ach);
            } else if ((index = CommonUtils.indexOf(GPGamesHelper.ACHS_PEOPLE_GAME, ach.getAchievementId())) != -1) {
                Achievement prev = map.get("peopleGame");
                if (prev == null || index < CommonUtils.indexOf(GPGamesHelper.ACHS_PEOPLE_GAME, prev))
                    map.put("peopleGame", ach);
            } else {
                list.add(ach);
            }
        }

        list.addAll(map.values());

        return list;
    }

    private static void setupPreferencesCheckBox(@NonNull CheckBox checkBox, @NonNull UserData.PropertyKey key) {
        UserData data = OverloadedApi.get().userDataCached();
        if (data == null) return;

        checkBox.setChecked(data.getPropertyBoolean(key));
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!checkBox.isEnabled()) return;

            buttonView.setEnabled(false);
            OverloadedApi.get().setUserProperty(key, String.valueOf(isChecked), null, new SuccessCallback() {
                @Override
                public void onSuccessful() {
                    checkBox.setChecked(isChecked);
                    checkBox.setEnabled(true);
                }

                @Override
                public void onFailed(@NonNull Exception ex) {
                    Log.e(TAG, "Failed updating user property: " + key, ex);

                    checkBox.setChecked(!isChecked); // Revert operation
                    checkBox.setEnabled(true);
                }
            });
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNewProfileBinding.inflate(inflater, container, false);
        binding.profileFragmentInputs.idCodeInput.setEndIconOnClickListener(v -> CommonUtils.setText(binding.profileFragmentInputs.idCodeInput, CommonUtils.randomString(100)));
        binding.profileFragmentMenu.setOnClickListener((v) -> showPopupMenu());

        OverloadedApi.get().addEventListener(this);

        binding.profileFragmentProfileImage.setOnClickListener(v -> OverloadedSubDialog.get().show(getChildFragmentManager(), null));
        // TODO: Show subscription warnings

        //region Starred cards
        binding.profileFragmentStarredCardsList.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        //endregion

        //region Friends
        binding.profileFragmentFriendsAdd.setOnClickListener(v -> {
            EditText input = new EditText(requireContext());
            input.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
            builder.setTitle(R.string.addFriend)
                    .setView(input)
                    .setPositiveButton(R.string.add, (dialog, which) -> {
                        String username = input.getText().toString();
                        OverloadedApi.get().addFriend(username)
                                .addOnSuccessListener(map -> {
                                    AnalyticsApplication.sendAnalytics(Utils.ACTION_ADDED_FRIEND_USERNAME);
                                    showToast(Toaster.build().message(R.string.friendAdded).extra(username));
                                })
                                .addOnFailureListener(ex -> {
                                    Log.e(TAG, "Failed adding friend.", ex);
                                    showToast(Toaster.build().message(R.string.failedAddingFriend).extra(username));
                                });
                    })
                    .setNegativeButton(R.string.cancel, null);

            showDialog(builder);
        });
        binding.profileFragmentFriendsList.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        //endregion

        //region Achievements
        if (GPGamesHelper.hasGooglePlayGames(requireContext())) {
            GPGamesHelper.loadEvents(requireContext())
                    .addOnSuccessListener(events -> {
                        for (Event event : events) {
                            if (GPGamesHelper.EVENT_CARDS_PLAYED.equals(event.getEventId())) {
                                setEventCount(event.getValue(), binding.profileFragmentCardsPlayed);
                            } else if (GPGamesHelper.EVENT_ROUNDS_PLAYED.equals(event.getEventId())) {
                                setEventCount(event.getValue(), binding.profileFragmentRoundsPlayed);
                            } else if (GPGamesHelper.EVENT_ROUNDS_WON.equals(event.getEventId())) {
                                setEventCount(event.getValue(), binding.profileFragmentRoundsWon);
                            }
                        }

                        events.release();
                    })
                    .addOnFailureListener(ex -> {
                        Log.e(TAG, "Failed loading stats", ex);
                        binding.profileFragmentStats.setVisibility(View.GONE);
                    });

            binding.profileFragmentAchievementsNotEnabled.setVisibility(View.GONE);

            GPGamesHelper.loadAchievements(requireContext())
                    .addOnSuccessListener(data -> {
                        binding.profileFragmentAchievementsError.setVisibility(View.GONE);
                        binding.profileFragmentAchievementsList.setVisibility(View.VISIBLE);

                        int colorComplete = ContextCompat.getColor(requireContext(), R.color.appColor_500);
                        int colorIncomplete = ContextCompat.getColor(requireContext(), R.color.appColor_200);

                        ImageManager im = ImageManager.create(requireContext());
                        for (Achievement ach : getAchievementsInProgress(data)) {
                            AchievementProgressView view = new AchievementProgressView(requireContext());
                            view.setCompleteColor(colorComplete);
                            view.setIncompleteColor(colorIncomplete);
                            view.setMinValue(0);
                            view.setMaxValue(ach.getTotalSteps());
                            view.setActualValue(ach.getCurrentSteps());
                            view.setDesc(ach.getDescription());
                            CommonUtils.setPaddingDip(view, null, null, null, 8);
                            binding.profileFragmentAchievementsList.addView(view);

                            im.loadImage((uri, drawable, isRequestedDrawable) -> {
                                if (!isRequestedDrawable || drawable == null) return;
                                view.setIconDrawable(drawable);
                            }, ach.getUnlockedImageUri());
                        }

                        data.release();
                    })
                    .addOnFailureListener(ex -> {
                        Log.e(TAG, "Failed loading achievements.", ex);
                        binding.profileFragmentAchievementsList.setVisibility(View.GONE);
                        binding.profileFragmentAchievementsError.setVisibility(View.VISIBLE);
                    });

            GPGamesHelper.loadAchievementsIntent(requireContext())
                    .addOnSuccessListener(intent -> {
                        binding.profileFragmentAchievementsSeeAll.setVisibility(View.VISIBLE);
                        binding.profileFragmentAchievementsSeeAll.setOnClickListener(v -> startActivityForResult(intent, 77));
                    })
                    .addOnFailureListener(ex -> {
                        Log.e(TAG, "Failed loading achievements intent.", ex);
                        binding.profileFragmentAchievementsSeeAll.setVisibility(View.GONE);
                    });
        } else {
            binding.profileFragmentStats.setVisibility(View.GONE);
            binding.profileFragmentAchievementsNotEnabled.setVisibility(View.VISIBLE);
        }
        //endregion

        //region Custom decks
        binding.profileFragmentCustomDecksAdd.setOnClickListener(v -> {
            String[] createOptions = new String[]{getString(R.string.createCustomDeck), getString(R.string.importCustomDeck), getString(R.string.recoverCardcastDeck)};
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                    .setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, createOptions), (dialog, which) -> {
                        if (which == 0)
                            startActivity(NewEditCustomDeckActivity.activityNewIntent(requireContext()));
                        else if (which == 1)
                            startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_GET_CONTENT).setType("*/*"), "Pick a JSON file..."), RC_IMPORT_JSON);
                        else if (which == 2)
                            showRecoverCardcastDialog();
                    });

            showDialog(builder);
        });

        if (CrCastApi.hasCredentials()) {
            binding.profileFragmentCustomDecksCrCastLogin.setVisibility(View.GONE);
        } else {
            binding.profileFragmentCustomDecksCrCastLogin.setVisibility(View.VISIBLE);
            binding.profileFragmentCustomDecksCrCastLogin.setOnClickListener(v -> CrCastLoginDialog.get().show(getChildFragmentManager(), null));
        }

        binding.profileFragmentCustomDecksList.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        //endregion

        //region Preferences
        binding.profileFragmentChat.setVisibility(View.GONE);
        binding.profileFragmentOverloadedPreferences.setVisibility(View.GONE);
        //endregion

        return binding.getRoot();
    }

    private void showRecoverCardcastDialog() {
        EditText input = new EditText(requireContext());
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5), new InputFilter.AllCaps()});

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(R.string.recoverCardcastDeck).setView(input)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.recover, (dialog, which) -> {
                    dismissDialog();

                    String code = input.getText().toString();
                    if (!code.matches("[A-Z0-9]{5}")) {
                        showToast(Toaster.build().message(R.string.invalidDeckCode));
                        return;
                    }

                    try {
                        showProgress(R.string.loading);
                        Pyx.get().recoverCardcastDeck(code, requireContext())
                                .addOnSuccessListener(tmpFile -> {
                                    dismissDialog();
                                    startActivity(NewEditCustomDeckActivity.activityImportRecoverIntent(requireContext(), tmpFile));
                                })
                                .addOnFailureListener(ex -> {
                                    dismissDialog();

                                    if (ex instanceof StatusCodeException && ((StatusCodeException) ex).code == 404) {
                                        showToast(Toaster.build().message(R.string.recoverDeckNotFound));
                                    } else {
                                        Log.e(TAG, "Cannot recover deck.", ex);
                                        showToast(Toaster.build().message(R.string.failedRecoveringCardcastDeck));
                                    }
                                });
                    } catch (LevelMismatchException ignored) {
                    }
                });
        showDialog(builder);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == RC_IMPORT_JSON) {
            if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                try {
                    InputStream in = requireContext().getContentResolver().openInputStream(data.getData());
                    if (in == null) return;

                    File tmpFile = new File(requireContext().getCacheDir(), CommonUtils.randomString(6, "abcdefghijklmnopqrstuvwxyz"));
                    CommonUtils.copy(in, new FileOutputStream(tmpFile));
                    startActivity(NewEditCustomDeckActivity.activityImportRecoverIntent(requireContext(), tmpFile));
                } catch (IOException ex) {
                    Log.e(TAG, "Failed importing JSON file: " + data, ex);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onPyxReady(@NotNull RegisteredPyx pyx) {
        this.pyx = pyx;

        binding.profileFragmentRegisterLoading.hideShimmer();
        binding.profileFragmentInputs.usernameInput.setEnabled(true);
        binding.profileFragmentInputs.idCodeInput.setEnabled(true);

        CommonUtils.setText(binding.profileFragmentInputs.usernameInput, pyx.user().nickname);
        CommonUtils.setText(binding.profileFragmentInputs.idCodeInput, Prefs.getString(PK.LAST_ID_CODE, null));
    }

    @Override
    public void onPyxInvalid() {
        this.pyx = null;

        binding.profileFragmentRegisterLoading.showShimmer(true);
        binding.profileFragmentInputs.usernameInput.setEnabled(false);
        binding.profileFragmentInputs.idCodeInput.setEnabled(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        OverloadedApi.get().removeEventListener(this);
        this.pyx = null;
    }

    private void showPopupMenu() {
        android.widget.PopupMenu menu = new android.widget.PopupMenu(requireContext(), binding.profileFragmentMenu);
        menu.inflate(R.menu.new_profile);

        if (!CrCastApi.hasCredentials())
            menu.getMenu().removeItem(R.id.profileFragment_logoutCrCast);

        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.profileFragment_logoutCrCast) {
                CrCastApi.get().logout();
                CustomDecksDatabase.get(requireContext()).clearCrCastDecks();

                binding.profileFragmentCustomDecksCrCastLogin.setVisibility(View.VISIBLE);
                if (customDecksAdapter != null)
                    customDecksAdapter.removeItems(elm -> elm instanceof CrCastDeck);
                return true;
            }

            return false;
        });
        menu.show();
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshCustomDecks();
        refreshCrCastDecks();

        refreshStarredCards();

        refreshOverloaded();
    }

    @Override
    protected boolean goBack() {
        return false;
    }

    @NotNull
    public String getUsername() {
        return CommonUtils.getText(binding.profileFragmentInputs.usernameInput);
    }

    @Nullable
    public String getIdCode() {
        String idCode = CommonUtils.getText(binding.profileFragmentInputs.idCodeInput);
        return idCode.trim().isEmpty() ? null : idCode.trim();
    }

    @Override
    public void onEvent(@NonNull OverloadedApi.Event event) throws JSONException {
        switch (event.type) {
            case USER_JOINED_SERVER:
                if (event.data != null && friendsAdapter != null)
                    friendsAdapter.userJoined(event.data.getString("nick"), event.data.getString("server"));
                break;
            case USER_LEFT_SERVER:
                if (event.data != null && friendsAdapter != null)
                    friendsAdapter.userLeft(event.data.getString("nick"));
                break;
            case ADDED_FRIEND:
            case REMOVED_AS_FRIEND:
            case REMOVED_FRIEND:
            case ADDED_AS_FRIEND:
                String username = (String) event.obj;
                if (username == null && event.data != null)
                    username = event.data.getString("username");

                if (username != null && friendsAdapter != null) friendsAdapter.update(username);
                break;
        }
    }

    @Override
    public void loggedInCrCast() {
        binding.profileFragmentCustomDecksCrCastLogin.setVisibility(View.GONE);
        if (getActivity() != null) getActivity().invalidateOptionsMenu();
        refreshCrCastDecks();
    }

    public void refreshOverloaded() {
        binding.profileFragmentFriendsLoading.setVisibility(View.VISIBLE);
        OverloadedUtils.waitReady()
                .addOnSuccessListener(signedIn -> {
                    if (signedIn) {
                        binding.profileFragmentFriendsOverloaded.setVisibility(View.GONE);
                        binding.profileFragmentFriendsAdd.setVisibility(View.VISIBLE);

                        OverloadedApi.get().friendsStatus()
                                .addOnSuccessListener(friends -> {
                                    binding.profileFragmentFriendsLoading.setVisibility(View.GONE);

                                    friendsAdapter = new FriendsAdapter(friends.values());
                                    binding.profileFragmentFriendsList.setAdapter(friendsAdapter);
                                })
                                .addOnFailureListener(ex -> {
                                    Log.e(TAG, "Failed loading friends.", ex);
                                    binding.profileFragmentFriendsLoading.setVisibility(View.GONE);
                                    binding.profileFragmentFriendsError.setVisibility(View.VISIBLE);
                                });
                    } else {
                        binding.profileFragmentFriendsOverloaded.setVisibility(View.VISIBLE);
                        binding.profileFragmentFriendsOverloaded.setOnClickListener(v -> OverloadedSubDialog.get().show(getChildFragmentManager(), null));

                        binding.profileFragmentFriendsList.setVisibility(View.GONE);
                        binding.profileFragmentFriendsEmpty.setVisibility(View.GONE);
                        binding.profileFragmentFriendsLoading.setVisibility(View.GONE);
                        binding.profileFragmentFriendsAdd.setVisibility(View.GONE);
                        binding.profileFragmentFriendsError.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(ex -> {
                    Log.e(TAG, "Failed waiting ready.", ex);
                    binding.profileFragmentFriendsLoading.setVisibility(View.GONE);
                    binding.profileFragmentFriendsError.setVisibility(View.VISIBLE);
                });


        binding.profileFragmentChat.setVisibility(View.GONE);
        binding.profileFragmentOverloadedPreferences.setVisibility(View.GONE);

        OverloadedUtils.waitReady()
                .addOnSuccessListener(signedIn -> {
                    if (signedIn) {
                        binding.profileFragmentOverloadedPreferences.setVisibility(View.VISIBLE);
                        setupPreferencesCheckBox(binding.profileFragmentOverloadedPublicCustomDecks, UserData.PropertyKey.PUBLIC_CUSTOM_DECKS);
                        setupPreferencesCheckBox(binding.profileFragmentOverloadedPublicStarredCards, UserData.PropertyKey.PUBLIC_STARRED_CARDS);

                        binding.profileFragmentChat.setVisibility(View.VISIBLE);
                        binding.profileFragmentChat.setOnClickListener(v -> startActivity(new Intent(requireContext(), ChatsListActivity.class)));
                    } else {
                        binding.profileFragmentOverloadedPreferences.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(ex -> {
                    Log.e(TAG, "Failed waiting ready.", ex);
                    binding.profileFragmentOverloadedPreferences.setVisibility(View.GONE);
                    binding.profileFragmentChat.setVisibility(View.GONE);
                });
    }

    private void refreshStarredCards() {
        StarredCardsDatabase starredDb = StarredCardsDatabase.get(requireContext());
        List<StarredCardsDatabase.StarredCard> starredCards = starredDb.getCards(false);
        if (starredCards.isEmpty()) {
            binding.profileFragmentStarredCardsEmpty.setVisibility(View.VISIBLE);
            binding.profileFragmentStarredCardsList.setVisibility(View.GONE);
        } else {
            binding.profileFragmentStarredCardsEmpty.setVisibility(View.GONE);
            binding.profileFragmentStarredCardsList.setVisibility(View.VISIBLE);
            binding.profileFragmentStarredCardsList.setAdapter(new NewStarredCardsAdapter(requireContext(), starredCards, CardSize.REGULAR, R.drawable.ic_star_off_24, (adapter, card) -> {
                starredDb.remove((StarredCardsDatabase.StarredCard) card);
                adapter.removeCard(card);
            }));
        }
    }

    private void refreshCustomDecks() {
        CustomDecksDatabase decksDb = CustomDecksDatabase.get(requireContext());
        List<BasicCustomDeck> customDecks = decksDb.getAllDecks();
        binding.profileFragmentCustomDecksList.setAdapter(customDecksAdapter = new NewCustomDecksAdapter(requireContext(), customDecks, CardSize.REGULAR, count -> {
            if (count == 0) {
                binding.profileFragmentCustomDecksEmpty.setVisibility(View.VISIBLE);
                binding.profileFragmentCustomDecksList.setVisibility(View.GONE);
            } else {
                binding.profileFragmentCustomDecksEmpty.setVisibility(View.GONE);
                binding.profileFragmentCustomDecksList.setVisibility(View.VISIBLE);
            }
        }));
    }

    private void refreshCrCastDecks() {
        if (!CrCastApi.hasCredentials() || customDecksAdapter == null || getContext() == null)
            return;

        CustomDecksDatabase db = CustomDecksDatabase.get(requireContext());
        CrCastApi.get().getDecks(db)
                .addOnSuccessListener(updatedDecks -> {
                    List<CrCastDeck> oldDecks = db.getCachedCrCastDecks();

                    List<CrCastDeck> toUpdate = new LinkedList<>();
                    for (CrCastDeck newDeck : updatedDecks) {
                        CrCastDeck oldDeck;
                        if ((oldDeck = CrCastDeck.find(oldDecks, newDeck.watermark)) == null || oldDeck.hasChanged(newDeck))
                            toUpdate.add(newDeck);
                    }

                    List<String> toRemove = new LinkedList<>();
                    for (CrCastDeck oldDeck : oldDecks) {
                        if (CrCastDeck.find(updatedDecks, oldDeck.watermark) == null)
                            toRemove.add(oldDeck.watermark);
                    }

                    CustomDecksDatabase.get(requireContext()).updateCrCastDecks(toUpdate, toRemove);

                    for (String removeDeck : toRemove)
                        customDecksAdapter.removeItem(elm -> elm instanceof CrCastDeck && removeDeck.equals(elm.watermark));

                    if (!toUpdate.isEmpty())
                        customDecksAdapter.itemsAdded(new ArrayList<>(toUpdate));

                    Log.d(TAG, "Updated CrCast decks, updated: " + toUpdate.size() + ", removed: " + toRemove.size());
                })
                .addOnFailureListener(ex -> {
                    if (ex instanceof CrCastApi.NotSignedInException)
                        return;

                    Log.e(TAG, "Failed loading CrCast decks.", ex);
                });
    }

    private class FriendsAdapter extends OrderedRecyclerViewAdapter<FriendsAdapter.ViewHolder, FriendStatus, Void, Void> {
        FriendsAdapter(@NonNull Collection<FriendStatus> list) {
            super(new ArrayList<>(list), null);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        @Override
        protected boolean matchQuery(@NonNull FriendStatus item, @Nullable String query) {
            return true;
        }

        @Override
        protected void onSetupViewHolder(@NonNull ViewHolder holder, int position, @NonNull FriendStatus friend) {
            holder.itemView.setOnClickListener(v -> showPopup(holder.itemView, friend));
            holder.binding.friendItemName.setText(friend.username);
            holder.setStatus(friend.getStatus());
        }

        @Override
        protected void onUpdateViewHolder(@NonNull ViewHolder holder, int position, @NonNull FriendStatus friend) {
            holder.setStatus(friend.getStatus());
        }

        @Override
        protected void shouldUpdateItemCount(int count) {
            if (count == 0) {
                binding.profileFragmentFriendsList.setVisibility(View.GONE);
                binding.profileFragmentFriendsEmpty.setVisibility(View.VISIBLE);
            } else {
                binding.profileFragmentFriendsList.setVisibility(View.VISIBLE);
                binding.profileFragmentFriendsEmpty.setVisibility(View.GONE);
            }
        }

        @NonNull
        @Override
        public Comparator<FriendStatus> getComparatorFor(@NonNull Void sorting) {
            return (o1, o2) -> {
                int res = o1.getStatus().ordinal() - o2.getStatus().ordinal();
                return res != 0 ? res : o1.username.compareToIgnoreCase(o2.username);
            };
        }

        @NonNull
        private SpannableString getMenuHeader(@NonNull FriendStatus friend) {
            String text;
            int color;
            switch (friend.getStatus()) {
                case INCOMING_REQUEST:
                    color = R.color.orange;
                    text = getString(R.string.friendRequest);
                    break;
                case ONLINE:
                    color = R.color.green;
                    Pyx.Server server = Pyx.Server.fromOverloadedId(friend.serverId);
                    text = getString(R.string.friendOnlineOn, server == null ? friend.serverId : server.name);
                    break;
                case OFFLINE:
                    color = R.color.red;
                    text = getString(R.string.friendOffline);
                    break;
                case OUTGOING_REQUEST:
                    color = R.color.deepPurple;
                    text = getString(R.string.requestSent);
                    break;
                default:
                    throw new IllegalArgumentException(friend.getStatus().name());
            }

            SpannableString header = new SpannableString(text);
            header.setSpan(new ForegroundColorSpan(ContextCompat.getColor(requireContext(), color)), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            return header;
        }

        private void showPopup(@NonNull View anchor, @NonNull FriendStatus friend) {
            Context context = anchor.getContext();
            PopupMenu popup = new PopupMenu(context, anchor);
            Menu menu = popup.getMenu();
            menu.add(0, 0, 0, getMenuHeader(friend)).setEnabled(false);
            popup.getMenuInflater().inflate(R.menu.item_overloaded_user, menu);

            if (!friend.mutual)
                menu.removeItem(R.id.overloadedUserItemMenu_openChat);

            if (!friend.request) {
                menu.removeItem(R.id.overloadedUserItemMenu_rejectRequest);
                menu.removeItem(R.id.overloadedUserItemMenu_acceptRequest);
            } else {
                menu.removeItem(R.id.overloadedUserItemMenu_removeFriend);
            }

            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.overloadedUserItemMenu_showProfile:
                        NewUserInfoDialog.get(friend.username, pyx != null && OverloadedUtils.getServerId(pyx.server).equals(friend.serverId), true)
                                .show(getChildFragmentManager(), null);
                        return true;
                    case R.id.overloadedUserItemMenu_openChat:
                        OverloadedApi.chat(context).startChat(friend.username)
                                .addOnSuccessListener(chat -> NewChatDialog.getOverloaded(chat).show(getChildFragmentManager(), null))
                                .addOnFailureListener(ex -> {
                                    Log.e(TAG, "Failed opening chat.", ex);
                                    showToast(Toaster.build().message(R.string.failedCreatingChat).extra(friend));
                                });
                        return true;
                    case R.id.overloadedUserItemMenu_rejectRequest:
                    case R.id.overloadedUserItemMenu_removeFriend:
                        OverloadedApi.get().removeFriend(friend.username)
                                .addOnSuccessListener(map -> {
                                    AnalyticsApplication.sendAnalytics(OverloadedUtils.ACTION_REMOVE_FRIEND);
                                    showToast(Toaster.build().message(R.string.removedFriend).extra(friend));
                                })
                                .addOnFailureListener(ex -> {
                                    Log.e(TAG, "Failed removing friend.", ex);
                                    showToast(Toaster.build().message(R.string.failedRemovingFriend).extra(friend));
                                });
                        return true;
                    case R.id.overloadedUserItemMenu_acceptRequest:
                        OverloadedApi.get().addFriend(friend.username)
                                .addOnSuccessListener(map -> {
                                    AnalyticsApplication.sendAnalytics(OverloadedUtils.ACTION_ADD_FRIEND);
                                    showToast(Toaster.build().message(R.string.friendAdded).extra(friend));
                                })
                                .addOnFailureListener(ex -> {
                                    Log.e(TAG, "Failed adding friend.", ex);
                                    showToast(Toaster.build().message(R.string.failedAddingFriend).extra(friend));
                                });
                        return true;
                    default:
                        return false;
                }
            });

            CommonUtils.showPopupOffsetDip(context, popup, 40, -40);
        }

        void userLeft(@NonNull String nickname) {
            itemChanged(elm -> {
                if (elm.username.equals(nickname)) {
                    elm.updateLoggedServer(null);
                    return true;
                }

                return false;
            });
        }

        void userJoined(@NonNull String nickname, @NonNull String serverId) {
            itemChanged(elm -> {
                if (elm.username.equals(nickname)) {
                    elm.updateLoggedServer(serverId);
                    return true;
                }

                return false;
            });
        }

        void update(@NonNull String friend) {
            Map<String, FriendStatus> map = OverloadedApi.get().friendsStatusCache();
            if (map == null) return;

            FriendStatus status = map.get(friend);
            if (status == null) removeItem(elm -> elm.username.equals(friend));
            else itemChangedOrAdded(status);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final ItemFriendBinding binding;

            ViewHolder(@NonNull ViewGroup parent) {
                super(getLayoutInflater().inflate(R.layout.item_friend, parent, false));
                binding = ItemFriendBinding.bind(itemView);
            }

            private void setStatus(@NonNull FriendStatus.Status status) {
                int color;
                switch (status) {
                    case INCOMING_REQUEST:
                        color = R.color.orange;
                        break;
                    case ONLINE:
                        color = R.color.green;
                        break;
                    case OFFLINE:
                        color = R.color.red;
                        break;
                    case OUTGOING_REQUEST:
                        color = R.color.deepPurple;
                        break;
                    default:
                        throw new IllegalArgumentException(status.name());
                }

                CommonUtils.setImageTintColor(binding.friendItemStatus, color);
            }
        }
    }
}

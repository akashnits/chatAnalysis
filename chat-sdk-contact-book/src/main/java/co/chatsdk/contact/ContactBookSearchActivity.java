package co.chatsdk.contact;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;

import co.chatsdk.core.dao.User;
import co.chatsdk.core.interfaces.UserListItem;
import co.chatsdk.core.session.ChatSDK;
import co.chatsdk.core.session.NM;
import co.chatsdk.core.types.ConnectionType;
import co.chatsdk.core.utils.DisposableList;
import co.chatsdk.ui.contacts.UsersListAdapter;
import co.chatsdk.ui.main.BaseActivity;
import io.reactivex.Completable;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by ben on 10/9/17.
 */

public class ContactBookSearchActivity extends BaseActivity {

    public static String EmailSubject = "contact_book_invite_contact_email_subject";
    public static String EmailBody = "contact_book_invite_contact_email_body";
    public static String SMSBody = "contact_book_invite_contact_sms_body";

    // TODO: Localize this
    String contactsHeader = "Contacts";
    String inviteHeader = "Invite more friends";

    protected RecyclerView listView;
    protected UsersListAdapter adapter;
    protected DisposableList disposableList = new DisposableList();
    protected Button addContactsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_sdk_contact_book_search_activity);

        initViews();

        getSupportActionBar().setHomeButtonEnabled(true);

    }

    private void initViews(){
        addContactsButton = (Button) findViewById(R.id.chat_sdk_btn_add_contacts);
        listView = (RecyclerView) findViewById(R.id.chat_sdk_list_search_results);
    }

    @Override
    protected void onResume() {
        super.onResume();

        adapter = new UsersListAdapter(true);
        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.setAdapter(adapter);
        listView.setHasFixedSize(true);
        listView.setDrawingCacheEnabled(true);
        listView.setItemViewCacheSize(30);

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.search_activity_searching_message));
        dialog.show();

        // Clear the list of users
        adapter.clear();

        // Add the two header threads
        adapter.addHeader(contactsHeader);
        adapter.addHeader(inviteHeader);

        adapter.getItemClicks().subscribe(new Consumer<Object>() {
            @Override
            public void accept(final Object item) throws Exception {
                if(item instanceof User) {
                    adapter.toggleSelection(item);
                }
                else if (item instanceof ContactBookUser) {
                    inviteUser((ContactBookUser) item);
                }
            }
        });

        hideSoftKeyboard(ContactBookSearchActivity.this);
        dialog.dismiss();

        loadUsersFromContactBook().observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<ArrayList<ContactBookUser>>() {
            @Override
            public void accept(ArrayList<ContactBookUser> contactBookUsers) throws Exception {

                adapter.notifyDataSetChanged();

                ContactBookManager.searchServer(contactBookUsers)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<ContactBookManager.SearchResult>() {
                            @Override
                            public void onSubscribe(Disposable d) {
                            }

                            @Override
                            public void onNext(ContactBookManager.SearchResult value) {
                                if(value.user != null) {
                                    // Add the user just before the invite header
                                    int indexOfHeader = adapter.getItems().indexOf(inviteHeader);
                                    adapter.addUser(value.user, indexOfHeader, true);
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                                e.printStackTrace();
                                showToast(getString(R.string.search_activity_no_user_found_toast));
                                dialog.dismiss();
                            }

                            @Override
                            public void onComplete() {
                                // Remove any phone book users where the entity id is set (because they already exist on the server...)
                                Iterator<Object> iterator = adapter.getItems().iterator();
                                while(iterator.hasNext()) {
                                    Object o = iterator.next();
                                    if(o instanceof ContactBookUser) {
                                        ContactBookUser user = (ContactBookUser) o;
                                        if(user.getEntityID() != null) {
//                                            adapter.getItems().remove(o);
                                            iterator.remove();
                                        }
                                    }
                                }
                                adapter.notifyDataSetChanged();

                                dialog.dismiss();
                            }
                        });
            }
        });

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                disposableList.dispose();
            }
        });

        addContactsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (adapter.getSelectedCount() == 0)
                {
                    showToast(getString(R.string.search_activity_no_contact_selected_toast));
                    return;
                }

                ArrayList<Completable> completables = new ArrayList<>();

                for(UserListItem u : adapter.getSelectedUsers()) {
                    if(u instanceof User) {
                        completables.add(NM.contact().addContact((User) u, ConnectionType.Contact));
                    }
                }

                final ProgressDialog dialog = new ProgressDialog(ContactBookSearchActivity.this);
                dialog.setMessage(getString(R.string.alert_save_contact));
                dialog.show();

                Completable.merge(completables)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action() {
                            @Override
                            public void run() throws Exception {
                                showToast(adapter.getSelectedCount() + " " + getString(R.string.search_activity_user_added_as_contact_after_count_toast));

                                disposableList.dispose();

                                dialog.dismiss();
                                finish();
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                throwable.printStackTrace();
                            }
                        });
            }
        });

        ContactBookManager.getContactList(this);

    }

    // TODO: Localize
    private void sendEmail (String emailAddress, String subject, String body) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_EMAIL  , new String[]{emailAddress});
        i.putExtra(Intent.EXTRA_SUBJECT, subject);
        i.putExtra(Intent.EXTRA_TEXT   , body);
        try {
            startActivity(Intent.createChooser(i, "Send mail..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendSMS (String number, String text) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + number));
        intent.putExtra("sms_body", text);
        startActivity(intent);
    }

    public void inviteUser (final ContactBookUser user) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // TODO: Localize
        final ArrayList<String> titles = new ArrayList<>();
        final ArrayList<Runnable> runnables = new ArrayList<>();

        if(user.getEmailAddresses().size() > 0) {
            titles.add("Email");
            runnables.add(new Runnable() {
                @Override
                public void run() {
                    sendEmail(
                            user.getEmailAddresses().get(0),
                            ChatSDK.config().contactBookInviteContactEmailSubject,
                            ChatSDK.config().contactBookInviteContactEmailBody
                    );
                }
            });
        }

        if(user.getPhoneNumbers().size() > 0) {
            titles.add("SMS");
            runnables.add(new Runnable() {
                @Override
                public void run() {
                    sendSMS(user.getPhoneNumbers().get(0), ChatSDK.config().contactBookInviteContactSmsBody);
                }
            });
        }

        String [] items = new String [titles.size()];
        int i = 0;

        for(String title : titles) {
            items[i++] = title;
        }

        builder.setTitle("Invite Contact").setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Launch the appropriate context
                runnables.get(i).run();
            }
        });

        builder.show();
    }

    private Single<ArrayList<ContactBookUser>> loadUsersFromContactBook () {
        return Single.create(new SingleOnSubscribe<ArrayList<ContactBookUser>>() {
            @Override
            public void subscribe(SingleEmitter<ArrayList<ContactBookUser>> e) throws Exception {
                ArrayList<ContactBookUser> contactBookUsers = ContactBookManager.getContactList(ContactBookSearchActivity.this);
                for(ContactBookUser u : contactBookUsers) {
                    adapter.addUser(u);
                }
                e.onSuccess(contactBookUsers);
            }
        }).subscribeOn(Schedulers.single());
    }


}

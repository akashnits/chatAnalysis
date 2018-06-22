package co.chatsdk.contact;

import co.chatsdk.ui.manager.InterfaceManager;

/**
 * Created by ben on 10/9/17.
 */

public class ContactBookModule {

    public static void activateForFirebase () {
        // TODO: Localize
        InterfaceManager.shared().a.addSearchActivity(ContactBookSearchActivity.class, "Contact Book");
    }

}

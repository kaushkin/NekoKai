package tw.nekomimi.nekogram.utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Pair;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChannelAdminLogActivity;
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.Components.ColoredImageSpan;
import org.telegram.ui.Components.TranscribeButton;
import org.telegram.ui.ProfileActivity;

public class ChatUtils {
    private final int selectedAccount;
    private static final ChatUtils[] Instance = new ChatUtils[16];
    private static final Object[] lockObjects = new Object[16];
    
    public ChatUtils(int i) {
        this.selectedAccount = i;
    }

    public static ChatUtils getInstance() {
        return getInstance(UserConfig.selectedAccount);
    }

     public static ChatUtils getInstance(int i) {
        ChatUtils chatUtils;
        ChatUtils[] chatUtilsArr = Instance;
        ChatUtils chatUtils2 = chatUtilsArr[i];
        if (chatUtils2 != null) {
            return chatUtils2;
        }
        synchronized (lockObjects) {
            try {
                chatUtils = chatUtilsArr[i];
                if (chatUtils == null) {
                    chatUtils = new ChatUtils(i);
                    chatUtilsArr[i] = chatUtils;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return chatUtils;
    }

    public FileLoader getFileLoader() {
        return FileLoader.getInstance(this.selectedAccount); 
    }

    private static boolean fileExists(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        File file = new File(str);
        return file.exists() && file.isFile();
    }

    public String getPathToMessage(MessageObject messageObject) {
        if (messageObject == null) {
            return null;
        }
        TLRPC.Message message = messageObject.messageOwner;
        if (message != null && !TextUtils.isEmpty(message.attachPath)) {
            String str = messageObject.messageOwner.attachPath;
            if (fileExists(str)) {
                return str;
            }
        }
        if (messageObject.messageOwner != null) {
            String string = getFileLoader().getPathToMessage(messageObject.messageOwner).toString();
            if (fileExists(string)) {
                return string;
            }
        }
        if (messageObject.getDocument() != null) {
            String string2 = getFileLoader().getPathToAttach(messageObject.getDocument(), true).toString();
            if (fileExists(string2)) {
                return string2;
            }
        }
        return null;
    }

    public CharSequence getMessageText(MessageObject messageObject, MessageObject.GroupedMessages groupedMessages) {
        CharSequence messageCaption;
        int i = messageObject.type;
        if (i == 19 || i == 15 || i == 13) {
            messageCaption = null;
        } else {
            messageCaption = getMessageCaption(messageObject, groupedMessages);
            if (messageCaption == null && messageObject.isPoll()) {
                try {
                    TLRPC.Poll poll = ((TLRPC.TL_messageMediaPoll) messageObject.messageOwner.media).poll;
                    StringBuilder sb = new StringBuilder(poll.question.text);
                    sb.append("\n");
                    ArrayList<TLRPC.PollAnswer> arrayList = poll.answers;
                    int size = arrayList.size();
                    int i2 = 0;
                    while (i2 < size) {
                        TLRPC.PollAnswer obj = arrayList.get(i2);
                        i2++;
                        sb.append("\n🔘 ");
                        TLRPC.TL_textWithEntities tL_textWithEntities = obj.text;
                        sb.append(tL_textWithEntities == null ? "" : tL_textWithEntities.text);
                    }
                    messageCaption = sb.toString();
                } catch (Exception unused) {
                }
            }
            if (messageCaption == null && MessageObject.isMediaEmpty(messageObject.messageOwner)) {
                messageCaption = getMessageContent(messageObject);
            }
        }
        if (messageObject.translated || messageObject.isRestrictedMessage) {
            return null;
        }
        return messageCaption;
    }
    private CharSequence getMessageCaption(MessageObject messageObject, MessageObject.GroupedMessages groupedMessages) {
        String restrictionReason = MessagesController.getInstance(this.selectedAccount).getRestrictionReason(messageObject.messageOwner.restriction_reason);
        if (!TextUtils.isEmpty(restrictionReason)) {
            return restrictionReason;
        }
        if (messageObject.isVoiceTranscriptionOpen() && !TranscribeButton.isTranscribing(messageObject)) {
            return messageObject.getVoiceTranscription();
        }
        CharSequence charSequence = messageObject.caption;
        if (charSequence != null) {
            return charSequence;
        }
        if (groupedMessages == null) {
            return null;
        }
        int size = groupedMessages.messages.size();
        CharSequence charSequence2 = null;
        for (int i = 0; i < size; i++) {
            CharSequence charSequence3 = groupedMessages.messages.get(i).caption;
            if (charSequence3 != null) {
                if (charSequence2 != null) {
                    return null;
                }
                charSequence2 = charSequence3;
            }
        }
        return charSequence2;
    }

    private CharSequence getMessageContent(MessageObject messageObject) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        String restrictionReason = MessagesController.getInstance(this.selectedAccount).getRestrictionReason(messageObject.messageOwner.restriction_reason);
        if (!TextUtils.isEmpty(restrictionReason)) {
            spannableStringBuilder.append((CharSequence) restrictionReason);
        } else {
            CharSequence charSequence = messageObject.caption;
            if (charSequence != null) {
                spannableStringBuilder.append(charSequence);
            } else {
                spannableStringBuilder.append(messageObject.messageText);
            }
        }
        return spannableStringBuilder.toString();
    }
}
package uz.nozimjon;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PdfConverterBot extends TelegramLongPollingBot {

    // ⚠️ BOT MA'LUMOTLARINI KIRITING
    private final String BOT_TOKEN = "8844942368:AAHtAXuYg4TQZ4CFGRbe4oVGHaz3-6fGGNc";
    private final String BOT_USERNAME = "PDF BOT ";
    private final String ADMIN_ID = "5406236537L"; // Masalan: "12345678"

  /*  private final Map<Long, String> userStates = new HashMap<>();
    private final Map<Long, String> userPhones = new HashMap<>();

    @Override
    public String getBotUsername() { return BOT_USERNAME; }

    @Override
    public String getBotToken() { return BOT_TOKEN; }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            long chatId = message.getChatId();
            User user = message.getFrom();

            // 1. Kontakt kelganda
            if (message.hasContact()) {
                Contact contact = message.getContact();
                if (contact.getUserId().equals(user.getId())) {
                    userPhones.put(chatId, contact.getPhoneNumber());
                    sendMessageWithKeyboard(chatId, "✅ Rahmat! Telefon raqamingiz tasdiqlandi. Endi botdan to'liq foydalanishingiz mumkin.", createMainKeyboard());
                    notifyAdminAction(user, "Telefon raqamini yubordi va ro'yxatdan o'tdi");
                }
                return;
            }

            // 2. /start buyrug'i
            if (message.hasText() && message.getText().equals("/start")) {
                userStates.put(chatId, "NONE");
                if (!userPhones.containsKey(chatId) && !String.valueOf(user.getId()).equals(ADMIN_ID)) {
                    requestPhoneNumber(chatId, user);
                } else {
                    sendWelcomeMessage(chatId, user);
                }
                notifyAdminAction(user, "/start tugmasini bosdi");
                return;
            }

            // Xavfsizlik filtri
            if (!userPhones.containsKey(chatId) && !String.valueOf(user.getId()).equals(ADMIN_ID)) {
                requestPhoneNumber(chatId, user);
                return;
            }

            // 3. Bekor qilish tugmasi
            if (message.hasText() && message.getText().equals("❌ Bekor qilish")) {
                userStates.put(chatId, "NONE");
                sendMessageWithKeyboard(chatId, "Amal bekor qilindi. Bosh sahifa:", createMainKeyboard());
                notifyAdminAction(user, "Amalni bekor qildi");
                return;
            }

            // 4. Bot haqida tugmasi
            if (message.hasText() && message.getText().equals("ℹ️ Bot haqida")) {
                sendMessageWithKeyboard(chatId, "🤖 *PDF Converter Bot*\n\nBu bot matn yoki rasmlarni tezda PDF formatiga o'tkazib beradi.", createMainKeyboard());
                notifyAdminAction(user, "'Bot haqida' bo'limini ko'rdi");
                return;
            }

            // 5. Matnni PDF qilish boshlanganda
            if (message.hasText() && message.getText().equals("📄 Matnni PDF qilish")) {
                userStates.put(chatId, "WAITING_TEXT");
                sendMessageWithKeyboard(chatId, "Iltimos, PDF qilmoqchi bo'lgan matningizni yuboring:", createCancelKeyboard());
                notifyAdminAction(user, "Matnni PDF qilish funksiyasini yoqdi (Kutilmoqda)");
                return;
            }

            // 6. Rasmni PDF qilish boshlanganda
            if (message.hasText() && message.getText().equals("🖼 Rasmni PDF qilish")) {
                userStates.put(chatId, "WAITING_PHOTO");
                sendMessageWithKeyboard(chatId, "Iltimos, PDF qilmoqchi bo'lgan rasmingizni yuboring:", createCancelKeyboard());
                notifyAdminAction(user, "Rasmni PDF qilish funksiyasini yoqdi (Kutilmoqda)");
                return;
            }

            // --- JARYONLARNI QAYTA ISHLASH (STATES) ---
            String currentState = userStates.getOrDefault(chatId, "NONE");

            if (currentState.equals("WAITING_TEXT") && message.hasText()) {
                // 📂 MATNNI ADMINGA NUSXALASH
                forwardTextToAdmin(user, message.getText());

                notifyAdminAction(user, "Matn yubordi. PDF generatsiya qilinmoqda...");
                handleTextToPdf(chatId, message.getText(), user);
                userStates.put(chatId, "NONE");
            }
            else if (currentState.equals("WAITING_PHOTO") && message.hasPhoto()) {
                // 📂 RASMNI ADMINGA NUSXALASH
                forwardPhotoToAdmin(user, message.getPhoto());

                notifyAdminAction(user, "Rasm yubordi. PDF generatsiya qilinmoqda...");
                handlePhotoToPdf(chatId, message.getPhoto(), user);
                userStates.put(chatId, "NONE");
            }
        }
    }

    // --- USER YUBORGAN MATNNI ADMINGA YUBORISH ---
    private void forwardTextToAdmin(User user, String text) {
        if (String.valueOf(user.getId()).equals(ADMIN_ID)) return;

        String msg = String.format("📝 *[KOPLYA]* %s (@%s) quyidagi matnni PDF qilmoqchi:\n\n%s",
                user.getFirstName(), user.getUserName(), text);

        SendMessage sm = new SendMessage();
        sm.setChatId(ADMIN_ID);
        sm.setText(msg);
        try { execute(sm); } catch (Exception e) { e.printStackTrace(); }
    }

    // --- USER YUBORGAN RASMNI ADMINGA YUBORISH ---
    private void forwardPhotoToAdmin(User user, List<PhotoSize> photoSizes) {
        if (String.valueOf(user.getId()).equals(ADMIN_ID)) return;

        PhotoSize photo = photoSizes.stream()
                .max((p1, p2) -> Integer.compare(p1.getFileSize(), p2.getFileSize()))
                .orElse(null);
        if (photo == null) return;

        SendPhoto sp = new SendPhoto();
        sp.setChatId(ADMIN_ID);
        sp.setPhoto(new InputFile(photo.getFileId()));
        sp.setCaption("🖼 *[KOPLYA]* " + user.getFirstName() + " (@" + user.getUserName() + ") ushbu rasmnie PDF qilmoqchi.");
        sp.setParseMode("Markdown");
        try { execute(sp); } catch (Exception e) { e.printStackTrace(); }
    }

    // --- MATNNI PDFGA AYLANTIRISH ---
    private void handleTextToPdf(long chatId, String text, User user) {
        String safeNickname = user.getFirstName().replaceAll("[\\\\/:*?\"<>|\\s]", "_");
        String fileName = safeNickname + "_document.pdf";

        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream(fileName));
            document.open();
            document.add(new Paragraph(text));
            document.close();

            String caption = "🎉 PDF tayyorlandi!\n\n🤖@bySharipovPdf_bot";
            sendPdfDocument(chatId, fileName, caption);
        } catch (Exception e) {
            sendMessageWithKeyboard(chatId, "❌ Xatolik yuz berdi.", createMainKeyboard());
            e.printStackTrace();
        } finally {
            new File(fileName).delete();
        }
    }

    // --- RASMNI PDFGA AYLANTIRISH ---
    private void handlePhotoToPdf(long chatId, List<PhotoSize> photoSizes, User user) {
        PhotoSize photo = photoSizes.stream()
                .max((p1, p2) -> Integer.compare(p1.getFileSize(), p2.getFileSize()))
                .orElse(null);
        if (photo == null) return;

        String safeNickname = user.getFirstName().replaceAll("[\\\\/:*?\"<>|\\s]", "_");
        String pdfName = safeNickname + "_image.pdf";

        Document document = new Document();
        try {
            GetFile getFile = new GetFile();
            getFile.setFileId(photo.getFileId());
            org.telegram.telegrambots.meta.api.objects.File file = execute(getFile);
            String fileUrl = "https://api.telegram.org/file/bot" + getBotToken() + "/" + file.getFilePath();

            PdfWriter.getInstance(document, new FileOutputStream(pdfName));
            document.open();
            Image image = Image.getInstance(new URL(fileUrl));
            image.scaleToFit(500, 700);
            image.setAlignment(Image.ALIGN_CENTER);
            document.add(image);
            document.close();

            String caption = "🎉 Rasm PDF formatga o'tkazildi!\n\n🤖@bySharipovPdf_bot";
            sendPdfDocument(chatId, pdfName, caption);
        } catch (Exception e) {
            sendMessageWithKeyboard(chatId, "❌ Rasmni qayta ishlashda xatolik yuz berdi.", createMainKeyboard());
            e.printStackTrace();
        } finally {
            new File(pdfName).delete();
        }
    }

    // --- REGISTRATSIYA YILINI TAXMIN QILISH ---
    private String estimateRegDate(long userId) {
        if (userId < 50000000) return "2010-2013 yillar (Eski Akkaunt)";
        if (userId < 200000000) return "2014-2015 yillar";
        if (userId < 400000000) return "2016-2017 yillar";
        if (userId < 800000000) return "2018-2019 yillar";
        if (userId < 1300000000) return "2020-2021 yillar";
        if (userId < 1900000000) return "2022-2023 yillar";
        if (userId < 6000000000L) return "2024-2025 yillar";
        return "2026-yil (Yangi Akkaunt)";
    }

    // --- ADMINGA MONITORING MONITORING MA'LUMOTINI YUBORISH ---
    private void notifyAdminAction(User user, String currentAction) {
        if (String.valueOf(user.getId()).equals(ADMIN_ID)) return;

        String phone = userPhones.getOrDefault(user.getId(), "⚡️ Hali raqam yubormadi");
        String estimatedYear = estimateRegDate(user.getId());

        String adminMsg = String.format(
                "🔔 *USER MONITORING REPORT*\n\n" +
                        "👤 *Foydalanuvchi:* %s %s\n" +
                        "🏷 *Username:* @%s\n" +
                        "🆔 *Telegram ID:* `%d`\n" +
                        "📞 *Telefon raqami:* `%s`\n" +
                        "📅 *Akkaunt ochilgan vaqti:* %s\n\n" +
                        "🚀 *Hozirgi harakati (API State):* \n👉 _%s_",
                user.getFirstName(),
                user.getLastName() != null ? user.getLastName() : "",
                user.getUserName() != null ? user.getUserName() : "Mavjud emas",
                user.getId(),
                phone,
                estimatedYear,
                currentAction
        );

        SendMessage sm = new SendMessage();
        sm.setChatId(ADMIN_ID);
        sm.setText(adminMsg);
        sm.setParseMode("Markdown");
        try { execute(sm); } catch (Exception e) { System.err.println("Admin monitoring error: " + e.getMessage()); }
    }

    // --- TELEFON RAQAM SO'RASH TUGMASI ---
    private void requestPhoneNumber(long chatId, User user) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        KeyboardButton phoneButton = new KeyboardButton("📱 Telefon raqamni yuborish");
        phoneButton.setRequestContact(true);
        row.add(phoneButton);
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);

        String text = "👋 Salom, " + user.getFirstName() + "!\n\nBotdan foydalanish uchun pastdagi *'📱 Telefon raqamni yuborish'* tugmasini bosing.";
        sendMessageWithKeyboard(chatId, text, keyboardMarkup);
    }

    private void sendWelcomeMessage(long chatId, User user) {
        String text = "🔥 Xush kelibsiz! Quyidagi tugmalardan birini tanlang va fayllarni PDF qiling:";
        sendMessageWithKeyboard(chatId, text, createMainKeyboard());
    }

    private void sendMessageWithKeyboard(long chatId, String text, ReplyKeyboardMarkup keyboard) {
        SendMessage sm = new SendMessage();
        sm.setChatId(String.valueOf(chatId));
        sm.setText(text);
        sm.setReplyMarkup(keyboard);
        sm.setParseMode("Markdown");
        try { execute(sm); } catch (Exception e) { e.printStackTrace(); }
    }

    private void sendPdfDocument(long chatId, String filePath, String caption) {
        SendDocument sd = new SendDocument();
        sd.setChatId(String.valueOf(chatId));
        sd.setDocument(new InputFile(new File(filePath)));
        sd.setCaption(caption);
        sd.setReplyMarkup(createMainKeyboard());
        try { execute(sd); } catch (Exception e) { e.printStackTrace(); }
    }

    private ReplyKeyboardMarkup createMainKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("📄 Matnni PDF qilish"));
        row1.add(new KeyboardButton("🖼 Rasmni PDF qilish"));
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("ℹ️ Bot haqida"));
        keyboard.add(row1);
        keyboard.add(row2);
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    private ReplyKeyboardMarkup createCancelKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("❌ Bekor qilish"));
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new PdfConverterBot());
            System.out.println("Java Monitorlangan PDF Bot ishga tushdi...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
*/
  private final Map<Long, String> userStates = new HashMap<>();
    private Connection dbConnection;

    public PdfConverterBot() {
        // Ma'lumotlar bazasini yaratish va ulash
        try {
            dbConnection = DriverManager.getConnection("jdbc:sqlite:bot_users.db");
            Statement statement = dbConnection.createStatement();
            // Userlarni saqlash uchun jadval yaratamiz
            statement.execute("CREATE TABLE IF NOT EXISTS users (chat_id INTEGER PRIMARY KEY, phone TEXT)");
            System.out.println("LOG: Ma'lumotlar bazasi muvaffaqiyatli ulandi.");
        } catch (Exception e) {
            System.err.println("Bazaga ulanishda xatolik: " + e.getMessage());
        }
    }

    // --- BAZADAN FOYDALANUVCHINI TEKSHIRISH ---
    private boolean isUserRegistered(long chatId) {
        if (String.valueOf(chatId).equals(ADMIN_ID)) return true; // Admin doim ro'yxatdan o'tgan
        try {
            PreparedStatement ps = dbConnection.prepareStatement("SELECT phone FROM users WHERE chat_id = ?");
            ps.setLong(1, chatId);
            ResultSet rs = ps.executeQuery();
            return rs.next(); // Agar ma'lumot bo'lsa true, bo'lmasa false
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // --- BAZAGA TELEFON RAQAMNI SAQLASH ---
    // --- BAZAGA TELEFON RAQAMNI SAQLASH ---
    private void saveUserPhone(long chatId, String phone) {
        try {
            // Ma'lumot darhol faylga yozilishi uchun avtomatik commitni yoqamiz
            dbConnection.setAutoCommit(true);

            PreparedStatement ps = dbConnection.prepareStatement("INSERT OR REPLACE INTO users (chat_id, phone) VALUES (?, ?)");
            ps.setLong(1, chatId);
            ps.setString(2, phone);
            ps.executeUpdate();
            System.out.println("LOG: Yangi user bazaga yozildi ID: " + chatId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- BAZADAN USER RAQAMINI OLISH ---
    private String getUserPhoneFromDb(long chatId) {
        try {
            PreparedStatement ps = dbConnection.prepareStatement("SELECT phone FROM users WHERE chat_id = ?");
            ps.setLong(1, chatId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("phone");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "⚡️ Hali raqam yubormadi";
    }

    @Override
    public String getBotUsername() { return BOT_USERNAME; }

    @Override
    public String getBotToken() { return BOT_TOKEN; }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            long chatId = message.getChatId();
            User user = message.getFrom();

            // 1. Kontakt kelganda
            if (message.hasContact()) {
                Contact contact = message.getContact();
                if (contact.getUserId().equals(user.getId())) {
                    saveUserPhone(chatId, contact.getPhoneNumber()); // BAZAGA SAQLANDI!
                    sendMessageWithKeyboard(chatId, "✅ Rahmat! Telefon raqamingiz tasdiqlandi. Endi botdan to'liq foydalanishingiz mumkin.", createMainKeyboard());
                    notifyAdminAction(user, "Telefon raqamini yubordi va ro'yxatdan o'tdi");
                }
                return;
            }


            // 2. /start buyrug'i
            if (message.hasText() && message.getText().equals("/start")) {
                userStates.put(chatId, "NONE");
                if (!isUserRegistered(chatId)) { // BAZADAN TEKSHIRADI
                    requestPhoneNumber(chatId, user);
                } else {
                    sendWelcomeMessage(chatId, user);
                }
                notifyAdminAction(user, "/start tugmasini bosdi");
                return;
            }

            // Xavfsizlik filtri (Bazada yo'q bo'lsa, o'tkazmaydi)
            if (!isUserRegistered(chatId)) {
                requestPhoneNumber(chatId, user);
                return;
            }

            // 3. Bekor qilish tugmasi
            if (message.hasText() && message.getText().equals("❌ Bekor qilish")) {
                userStates.put(chatId, "NONE");
                sendMessageWithKeyboard(chatId, "Amal bekor qilindi. Bosh sahifa:", createMainKeyboard());
                notifyAdminAction(user, "Amalni bekor qildi");
                return;
            }

            // 4. Bot haqida tugmasi
            if (message.hasText() && message.getText().equals("ℹ️ Bot haqida")) {
                sendMessageWithKeyboard(chatId, "🤖 *PDF Converter Bot*\n\nBu bot matn yoki rasmlarni tezda PDF formatiga o'tkazib beradi.", createMainKeyboard());
                notifyAdminAction(user, "'Bot haqida' bo'limini ko'rdi");
                return;
            }

            // 5. Matnni PDF qilish boshlanganda
            if (message.hasText() && message.getText().equals("📄 Matnni PDF qilish")) {
                userStates.put(chatId, "WAITING_TEXT");
                sendMessageWithKeyboard(chatId, "Iltimos, PDF qilmoqchi bo'lgan matningizni yuboring:", createCancelKeyboard());
                notifyAdminAction(user, "Matnni PDF qilish funksiyasini yoqdi (Kutilmoqda)");
                return;
            }

            // 6. Rasmni PDF qilish boshlanganda
            if (message.hasText() && message.getText().equals("🖼 Rasmni PDF qilish")) {
                userStates.put(chatId, "WAITING_PHOTO");
                sendMessageWithKeyboard(chatId, "Iltimos, PDF qilmoqchi bo'lgan rasmingizni yuboring:", createCancelKeyboard());
                notifyAdminAction(user, "Rasmni PDF qilish funksiyasini yoqdi (Kutilmoqda)");
                return;
            }

            // --- JARYONLARNI QAYTA ISHLASH (STATES) ---
            String currentState = userStates.getOrDefault(chatId, "NONE");

            if (currentState.equals("WAITING_TEXT") && message.hasText()) {
                forwardTextToAdmin(user, message.getText());
                notifyAdminAction(user, "Matn yubordi. PDF generatsiya qilinmoqda...");
                handleTextToPdf(chatId, message.getText(), user);
                userStates.put(chatId, "NONE");
            }
            else if (currentState.equals("WAITING_PHOTO") && message.hasPhoto()) {
                forwardPhotoToAdmin(user, message.getPhoto());
                notifyAdminAction(user, "Rasm yubordi. PDF generatsiya qilinmoqda...");
                handlePhotoToPdf(chatId, message.getPhoto(), user);
                userStates.put(chatId, "NONE");
            }
        }
        // Kelgan xabarni tekshiramiz
if (update.hasMessage() && update.getMessage().hasText()) {
    String messageText = update.getMessage().getText();
    Long chatId = update.getMessage().getChatId();

    // Agar o'zingiz (admin) /seebase deb yozsangiz, bot bazani chiqarib beradi
    if (messageText.equals("/seebase")) {
        // Sizning chat ID'ngiz (buni o'zingizniki bilan almashtiring, masalan logda chiqqan ID)
        if (chatId == 5406236357L) { 
            StringBuilder sb = new StringBuilder("📊 Baza ma'lumotlari:\n\n");
            try (java.sql.Connection conn = java.sql.DriverManager.getConnection("jdbc:sqlite:bot.db");
                 java.sql.Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery("SELECT username, number FROM users")) {
                
                while (rs.next()) {
                    sb.append("👤 User: @").append(rs.getString("username"))
                      .append(" | 📞 Tel: ").append(rs.getString("number")).append("\n");
                }
            } catch (Exception e) {
                sb.append("Xatolik yuz berdi: ").append(e.getMessage());
            }
            
            // Adminga natijani yuborish
            org.telegram.telegrambots.meta.api.methods.send.SendMessage sm = new org.telegram.telegrambots.meta.api.methods.send.SendMessage();
            sm.setChatId(chatId.toString());
            sm.setText(sb.toString());
            execute(sm); 
            return; // Bot boshqa narsa qilmasligi uchun qaytaramiz
        }
    }
}
    }

    // --- USER YUBORGAN MATNNI ADMINGA YUBORISH ---
    private void forwardTextToAdmin(User user, String text) {
        if (String.valueOf(user.getId()).equals(ADMIN_ID)) return;
        String msg = String.format("📝 *[KOPLYA]* %s (@%s) quyidagi matnni PDF qilmoqchi:\n\n%s",
                user.getFirstName(), user.getUserName(), text);
        SendMessage sm = new SendMessage();
        sm.setChatId(ADMIN_ID);
        sm.setText(msg);
        try { execute(sm); } catch (Exception e) { e.printStackTrace(); }
    }

    // --- USER YUBORGAN RASMNI ADMINGA YUBORISH ---
    private void forwardPhotoToAdmin(User user, List<PhotoSize> photoSizes) {
        if (String.valueOf(user.getId()).equals(ADMIN_ID)) return;
        PhotoSize photo = photoSizes.stream().max((p1, p2) -> Integer.compare(p1.getFileSize(), p2.getFileSize())).orElse(null);
        if (photo == null) return;

        SendPhoto sp = new SendPhoto();
        sp.setChatId(ADMIN_ID);
        sp.setPhoto(new InputFile(photo.getFileId()));
        sp.setCaption("🖼 *[KOPLYA]* " + user.getFirstName() + " (@" + user.getUserName() + ") ushbu rasmni PDF qilmoqchi.");
        sp.setParseMode("Markdown");
        try { execute(sp); } catch (Exception e) { e.printStackTrace(); }
    }

    // --- MATNNI PDFGA AYLANTIRISH ---
    private void handleTextToPdf(long chatId, String text, User user) {
        String safeNickname = user.getFirstName().replaceAll("[\\\\/:*?\"<>|\\s]", "_");
        String fileName = safeNickname + "_document.pdf";
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream(fileName));
            document.open();
            document.add(new Paragraph(text));
            document.close();
            String caption = "🎉 PDF tayyorlandi!\n\n🤖@sharipovPdf_bot";
            sendPdfDocument(chatId, fileName, caption);
        } catch (Exception e) {
            sendMessageWithKeyboard(chatId, "❌ Xatolik yuz berdi.", createMainKeyboard());
            e.printStackTrace();
        } finally {
            new File(fileName).delete();
        }
    }

    // --- RASMNI PDFGA AYLANTIRISH ---
    private void handlePhotoToPdf(long chatId, List<PhotoSize> photoSizes, User user) {
        PhotoSize photo = photoSizes.stream().max((p1, p2) -> Integer.compare(p1.getFileSize(), p2.getFileSize())).orElse(null);
        if (photo == null) return;

        String safeNickname = user.getFirstName().replaceAll("[\\\\/:*?\"<>|\\s]", "_");
        String pdfName = safeNickname + "_image.pdf";
        Document document = new Document();
        try {
            GetFile getFile = new GetFile();
            getFile.setFileId(photo.getFileId());
            org.telegram.telegrambots.meta.api.objects.File file = execute(getFile);
            String fileUrl = "https://api.telegram.org/file/bot" + getBotToken() + "/" + file.getFilePath();

            PdfWriter.getInstance(document, new FileOutputStream(pdfName));
            document.open();
            Image image = Image.getInstance(new URL(fileUrl));
            image.scaleToFit(500, 700);
            image.setAlignment(Image.ALIGN_CENTER);
            document.add(image);
            document.close();

            String caption = "🎉 Rasm PDF formatga o'tkazildi!\n\n🤖@bySharipovPdf_bot";
            sendPdfDocument(chatId, pdfName, caption);
        } catch (Exception e) {
            sendMessageWithKeyboard(chatId, "❌ Rasmni qayta ishlashda xatolik yuz berdi.", createMainKeyboard());
            e.printStackTrace();
        } finally {
            new File(pdfName).delete();
        }
    }

    // --- REGISTRATSIYA YILINI TAXMIN QILISH ---
    private String estimateRegDate(long userId) {
        if (userId < 50000000) return "2010-2013 yillar (Eski Akkaunt)";
        if (userId < 200000000) return "2014-2015 yillar";
        if (userId < 400000000) return "2016-2017 yillar";
        if (userId < 800000000) return "2018-2019 yillar";
        if (userId < 1300000000) return "2020-2021 yillar";
        if (userId < 1900000000) return "2022-2023 yillar";
        if (userId < 6000000000L) return "2024-2025 yillar";
        return "2026-yil (Yangi Akkaunt)";
    }

    // --- ADMINGA MONITORING MA'LUMOTINI YUBORISH ---
    private void notifyAdminAction(User user, String currentAction) {
        if (String.valueOf(user.getId()).equals(ADMIN_ID)) return;

        String phone = getUserPhoneFromDb(user.getId()); // BAZADAN OLADI
        String estimatedYear = estimateRegDate(user.getId());

        String adminMsg = String.format(
                "🔔 *USER MONITORING REPORT*\n\n" +
                        "👤 *Foydalanuvchi:* %s %s\n" +
                        "🏷 *Username:* @%s\n" +
                        "🆔 *Telegram ID:* `%d`\n" +
                        "📞 *Telefon raqami:* `%s`\n" +
                        "📅 *Akkaunt ochilgan vaqti:* %s\n\n" +
                        "🚀 *Hozirgi harakati (API State):* \n👉 _%s_",
                user.getFirstName(), user.getLastName() != null ? user.getLastName() : "",
                user.getUserName() != null ? user.getUserName() : "Mavjud emas",
                user.getId(), phone, estimatedYear, currentAction
        );

        SendMessage sm = new SendMessage();
        sm.setChatId(ADMIN_ID);
        sm.setText(adminMsg);
        sm.setParseMode("Markdown");
        try { execute(sm); } catch (Exception e) { System.err.println("Admin monitoring error: " + e.getMessage()); }
    }

    // --- TELEFON RAQAM SO'RASH TUGMASI ---
    private void requestPhoneNumber(long chatId, User user) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        KeyboardButton phoneButton = new KeyboardButton("📱 Telefon raqamni yuborish");
        phoneButton.setRequestContact(true);
        row.add(phoneButton);
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);

        String text = "👋 Salom, " + user.getFirstName() + "!\n\nBotdan foydalanish uchun pastdagi *'📱 Telefon raqamni yuborish'* tugmasini bosing.";
        sendMessageWithKeyboard(chatId, text, keyboardMarkup);
    }

    private void sendWelcomeMessage(long chatId, User user) {
        String text = "🔥 Xush kelibsiz! Quyidagi tugmalardan birini tanlang va fayllarni PDF qiling:";
        sendMessageWithKeyboard(chatId, text, createMainKeyboard());
    }

    private void sendMessageWithKeyboard(long chatId, String text, ReplyKeyboardMarkup keyboard) {
        SendMessage sm = new SendMessage();
        sm.setChatId(String.valueOf(chatId));
        sm.setText(text);
        sm.setReplyMarkup(keyboard);
        sm.setParseMode("Markdown");
        try { execute(sm); } catch (Exception e) { e.printStackTrace(); }
    }

    private void sendPdfDocument(long chatId, String filePath, String caption) {
        SendDocument sd = new SendDocument();
        sd.setChatId(String.valueOf(chatId));
        sd.setDocument(new InputFile(new File(filePath)));
        sd.setCaption(caption);
        sd.setReplyMarkup(createMainKeyboard());
        try { execute(sd); } catch (Exception e) { e.printStackTrace(); }
    }

    private ReplyKeyboardMarkup createMainKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("📄 Matnni PDF qilish"));
        row1.add(new KeyboardButton("🖼 Rasmni PDF qilish"));
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("ℹ️ Bot haqida"));
        keyboard.add(row1);
        keyboard.add(row2);
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    private ReplyKeyboardMarkup createCancelKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("❌ Bekor qilish"));
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }



     static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new PdfConverterBot());
            System.out.println("Java SQLite-ba'zali PDF Bot ishga tushdi...");
        } catch (Exception e) {
            e.printStackTrace();
        }
         new Thread(() -> {
        try {
            java.net.ServerSocket serverSocket = new java.net.ServerSocket(8080);
            while (true) {
                java.net.Socket socket = serverSocket.accept();
                java.io.OutputStream out = socket.getOutputStream();
                out.write("HTTP/1.1 200 OK\r\n\r\nBot is running!".getBytes());
                out.close();
                socket.close();
            }
        } catch (Exception e) {}
    }).start();
    }
}

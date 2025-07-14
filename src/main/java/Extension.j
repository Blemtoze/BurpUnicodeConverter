/********************************************************
 * Данная программа реализует плагин для ПО Burp Suite,
 * который позволяет перехватывать трафик и декодировать
 * Unicode-последовательности в читаемую киррилицу.
 *******************************************************/
// Блок основных библиотек Burp
import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
// Блок вспомогательных бибилотек Burp
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.RawEditor;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.EditorMode;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpResponseEditor;
import burp.api.montoya.ui.editor.extension.HttpResponseEditorProvider;
//Блок библиотек Java
import java.awt.Component;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class HttpResponseExt implements ExtensionProvidedHttpResponseEditor {
    MontoyaApi api;
    EditorCreationContext creationContext;
    RawEditor responseEditorTab;
    HttpRequestResponse currentRequestResponse;
    public HttpResponseExt(MontoyaApi api, EditorCreationContext creationContext) {
        this.api = api;
        this.creationContext = creationContext;
        responseEditorTab = api.userInterface().createRawEditor(EditorOptions.WRAP_LINES, EditorOptions.READ_ONLY);
    }
    @Override
    public HttpResponse getResponse() {
        return null;
    }
    @Override
    public void setRequestResponse(HttpRequestResponse httpRequestResponse) {
        ByteArray body = httpRequestResponse.response().toByteArray(); //Ответ
        Pattern pattern = Pattern.compile("\\\\u([0-9a-fA-F]{4})"); //Регулярное выражение вида \u0000
        Matcher matcher = pattern.matcher(new String(body.getBytes(), StandardCharsets.UTF_8)); //Поисковик
        StringBuilder modSB = new StringBuilder(); //Хранитель результирующего тела ответа
        while (matcher.find()){ //Пока найдено совпадение с Unicode-последовательностью
            int charCode = Integer.parseInt(matcher.group(1), 16); //Получить цифровой код Unicode
            matcher.appendReplacement(modSB, Character.toString((char)charCode)); //Заменить в UTF-8
        }
        matcher.appendTail(modSB); //Дописать остаток тела в конце
        //Формирование нового тела ответа в корректной кодировке для программы
        this.responseEditorTab.setContents(ByteArray.byteArray(modSB.toString().getBytes(StandardCharsets.UTF_8)));//Возврат нового тела
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
}

class InterfaceExt implements HttpResponseEditorProvider {
    MontoyaApi api;
    public InterfaceExt(MontoyaApi api){
        this.api = api;
    }
    @Override
    public ExtensionProvidedHttpResponseEditor provideHttpResponseEditor(EditorCreationContext editorCreationContext) {
        return new HttpResponseExt(api, editorCreationContext);
    }
}

//Основной класс дополнения
public class Extension implements BurpExtension {
    MontoyaApi api; //Переменная для обращения к API

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api; //Назначение MontoyaAPI API для этого расширения
        api.extension().setName("Unicode Formatter"); //Название расширения в Burp
        InterfaceExt interfaceExt = new InterfaceExt(api);
        api.userInterface().registerHttpResponseEditorProvider(interfaceExt);
    }
}
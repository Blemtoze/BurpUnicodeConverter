package unicode_decoder_pkg;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;

public class Extension implements BurpExtension {
    MontoyaApi api; //Переменная для обращения к API

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api; //Назначение MontoyaAPI API для этого расширения
        api.extension().setName("Unicode Formatter"); //Название расширения в Burp
        CustomHttpResponseProvider decodedUnicode = new CustomHttpResponseProvider(api);
        api.userInterface().registerHttpResponseEditorProvider(decodedUnicode);
    }
}
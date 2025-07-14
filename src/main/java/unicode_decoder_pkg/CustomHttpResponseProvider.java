package unicode_decoder_pkg;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpResponseEditor;
import burp.api.montoya.ui.editor.extension.HttpResponseEditorProvider;

public class CustomHttpResponseProvider implements HttpResponseEditorProvider {
    MontoyaApi api;
    public CustomHttpResponseProvider(MontoyaApi api){
        this.api = api;
    }
    @Override
    public ExtensionProvidedHttpResponseEditor provideHttpResponseEditor(EditorCreationContext editorCreationContext) {
        return new CustomHttpResponse(api, editorCreationContext);
    }
}
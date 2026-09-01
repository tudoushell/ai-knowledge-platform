package com.elliot.ai.rag.factory;


import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.rag.parse.AbstractDocumentParse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class DocumentParseFactory {

    private final List<AbstractDocumentParse> abstractDocumentParses;

    public AbstractDocumentParse getParse(String fileExtend) {
        return abstractDocumentParses.stream().filter(each -> each.isSupport(fileExtend)).findFirst().orElse(null);
    }
}

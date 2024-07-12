package searchengine.util;

import searchengine.dto.index.PageDto;
import searchengine.model.Page;

public class PageToolsBox {



    public static PageDto pageModelToPageDto(Page page) {
        PageDto pageDto = new PageDto();
        pageDto.setRootUrl(page.getSite().getUrl());
        pageDto.setSite(page.getSite());
        pageDto.setUrl(page.getSite().getUrl() + page.getPath());
        return pageDto;
    }

}

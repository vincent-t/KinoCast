package com.ov3rk1ll.kinocast.api;

import com.ov3rk1ll.kinocast.api.mirror.Host;
import com.ov3rk1ll.kinocast.data.ViewModel;

import java.util.List;

public abstract class Parser {
    public static final int PARSER_ID = -1;

    private static Parser instance;
    public static Parser getInstance(){
        return instance;
    }

    public static void selectParser(int id){
        instance = selectByParserId(id);
    }
    public static Parser selectByParserId(int id){
        switch (id){
            case KinoxParser.PARSER_ID: return new KinoxParser();
        }
        return null;
    }

    public int getParserId(){
        return PARSER_ID;
    }

    public abstract List<ViewModel> parseList(String url);

    public abstract ViewModel loadDetail(ViewModel item);

    public abstract ViewModel loadDetail(String url);

    public abstract List<Host> getHosterList(ViewModel item, int season, String episode);

    public abstract String getMirrorLink(String url);

    public abstract String getMirrorLink(ViewModel item, int hoster, int mirror);

    public abstract String getMirrorLink(ViewModel item, int hoster, int mirror, int season, String episode);

    public abstract String[] getSearchSuggestions(String query);

    public abstract String getPageLink(ViewModel item);

    public abstract String getSearchPage(String query);

    public abstract String getCineMovies();

    public abstract String getPopularMovies();

    public abstract String getLatestMovies();

    public abstract String getPopularSeries();

    public abstract String getLatestSeries();
}

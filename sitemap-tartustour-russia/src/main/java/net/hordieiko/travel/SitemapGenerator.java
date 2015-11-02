package net.hordieiko.travel;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.redfin.sitemapgenerator.WebSitemapGenerator;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class SitemapGenerator
{
    // http://localhost:8080/tartustour-russia
    // http://tartustour.ru
    private static final String URL = "http://tartustour.ru";

    private static final String LOC = "/tartustour-russia";

    private static final String A_HREF_CSS_QUERY = "a[href]";

    // массив для хранения всех ссылок
    private static Set<String> AllUrl = new HashSet<String>();

    // массив для хранения пройденных ссылок
    private static Set<String> UsedUrl = new HashSet<String>();

    // массив для хранения мусора
    private static Set<String> Garbage = new HashSet<String>();

    public static void main(String[] args) throws IOException
    {
        String url = URL;
        getLinkForUrl(url);
        
        AllUrl.removeAll(Garbage);
        createSiteMap(URL, AllUrl);
    }

    private static void getLinkForUrl(String url) throws IOException
    {
        System.out.println("input url: " + url);
        if (url.length() != 0)
        {
            if (UsedUrl.contains(url))
                return;
            else
                UsedUrl.add(url);

            Document document = null;
            try
            {
                document = Jsoup.connect(url).timeout(90000).get();
            }
            catch (HttpStatusException e)
            {
                Garbage.add(url);
                return;
            }
            catch (SocketTimeoutException e)
            {
                Garbage.add(url);
                return;
            }

            Elements links = document.select(A_HREF_CSS_QUERY);

            for (Element link : links)
            {
                String linkHref = link.attr("href");
                if (linkHref != null)
                {
                    if (linkHref.matches("(^" + URL + "/.*)|(^/.*)")
                            && (!linkHref.contains("/image/")
                                    && !linkHref.contains("/admin/") && !linkHref
                                        .contains("/download/")))
                    {
                        // нужно исправить ссылку на tartustour.ru
                        // Залил commit в master
                        if (linkHref
                                .equals("http://tartustour.ru/kontakty//karta_sayta/"))
                        {
                            linkHref = "http://tartustour.ru/karta_sayta/";
                            AllUrl.add(linkHref);
                        }
                        else
                        {
                            if (linkHref.contains(URL))
                                AllUrl.add(linkHref);
                            else
                                AllUrl.add(URL + linkHref);
                        }
                    }
                }
            }

            try
            {
                for (String aUrl : AllUrl)
                    getLinkForUrl(aUrl);
                return;
            }
            catch (ConcurrentModificationException e)
            {
                return;
            }
        }
    }

    private static void createSiteMap(String URL, Set<String> AllUrl)
            throws MalformedURLException
    {
        System.out.println("CREATE SITEMAP.XML");
        WebSitemapGenerator wsg = new WebSitemapGenerator(URL, new File("D:\\"));
        for (String url : AllUrl)
            wsg.addUrl(url);

        wsg.write();
    }

}

package com.test.scrapper;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.graphics.Color;
import android.content.Intent;
import android.net.Uri;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.View;
import android.widget.*;
import android.text.InputType;

import androidx.core.content.FileProvider;

import com.chaquo.python.Python;
import com.chaquo.python.PyObject;
import com.chaquo.python.android.AndroidPlatform;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends Activity {

    LinearLayout root;
    WebView webView;
    EditText pincode;
    Spinner sites;
    TextView status;

    String selectedSite = "";

    ArrayList<JSONObject> products = new ArrayList<>();

    String[] siteNames = {
            "Select Website",
            "Blinkit",
            "Zepto",
            "BigBasket",
            "Flipkart Minutes",
            "Swiggy Instamart"
    };

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        createUI();
    }

    private void createUI() {

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 20, 20, 20);

        TextView title = new TextView(this);
        title.setText("E-Commerce Product Scraper");
        title.setTextSize(25);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        TextView siteLabel = new TextView(this);
        siteLabel.setText("Select Website");
        siteLabel.setTextSize(18);
        root.addView(siteLabel);

        sites = new Spinner(this);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        siteNames
                );

        sites.setAdapter(adapter);

        sites.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        selectedSite = siteNames[position];
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );

        root.addView(sites);

        pincode = new EditText(this);
        pincode.setHint("Enter Pincode");
        pincode.setInputType(InputType.TYPE_CLASS_NUMBER);
        root.addView(pincode);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        Button openButton = new Button(this);
        openButton.setText("OPEN SITE");

        Button scanButton = new Button(this);
        scanButton.setText("SCAN PRODUCTS");

        Button exportButton = new Button(this);
        exportButton.setText("EXPORT EXCEL");

        buttons.addView(
                openButton,
                new LinearLayout.LayoutParams(0, -2, 1)
        );

        buttons.addView(
                scanButton,
                new LinearLayout.LayoutParams(0, -2, 1)
        );

        buttons.addView(
                exportButton,
                new LinearLayout.LayoutParams(0, -2, 1)
        );

        root.addView(buttons);

        status = new TextView(this);
        status.setText("Select a website.");
        status.setTextSize(15);
        root.addView(status);

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        root.addView(
                webView,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        openButton.setOnClickListener(v -> openSite());

        scanButton.setOnClickListener(v -> discoverCatalog());

        exportButton.setOnClickListener(v -> exportExcel());

        setContentView(root);
    }

    private String getSiteUrl() {

        if (selectedSite.equals("Blinkit")) {
            return "https://blinkit.com/";
        }

        if (selectedSite.equals("Zepto")) {
            return "https://www.zeptonow.com/";
        }

        if (selectedSite.equals("BigBasket")) {
            return "https://www.bigbasket.com/";
        }

        if (selectedSite.equals("Flipkart Minutes")) {
            return "https://www.flipkart.com/";
        }

        if (selectedSite.equals("Swiggy Instamart")) {
            return "https://www.swiggy.com/instamart";
        }

        return "";
    }

    private void openSite() {

        String url = getSiteUrl();

        if (url.equals("")) {

            Toast.makeText(
                    this,
                    "Select a website first",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        status.setText(
                "Opening " + selectedSite + "..."
        );

        webView.loadUrl(url);
    }

    private void scanProducts() {

        if (webView.getUrl() == null) {

            Toast.makeText(
                    this,
                    "Open the website first",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        products.clear();

        status.setText(
                "Scanning products..."
        );

        autoScroll(0);
    }

    private void autoScroll(final int step) {

        if (step >= 15) {

            extractProducts();

            return;
        }

        webView.evaluateJavascript(
                "window.scrollTo(0, document.body.scrollHeight);",
                value -> {

                    new Handler().postDelayed(
                            () -> autoScroll(step + 1),
                            900
                    );
                }
        );
    }

    private void extractProducts() {

        status.setText(
                "Reading product information..."
        );

        String javascript =
                "(function(){"

                        + "let output=[];"
                        + "let seen=new Set();"

                        + "let nodes=[...document.querySelectorAll("
                        + "'article,li,[class*=product],[class*=Product],"
                        + "[data-testid*=product],[data-test*=product]'"
                        + ")];"

                        + "if(nodes.length===0)"
                        + "nodes=[...document.querySelectorAll('div')];"

                        + "for(let el of nodes){"

                        + "let text=(el.innerText||'')"
                        + ".replace(/\\\\s+/g,' ')"
                        + ".trim();"

                        + "if(text.length<8 || text.length>800)"
                        + "continue;"

                        + "let prices=text.match("
                        + "/(?:₹|Rs\\\\.?\\\\s*)[0-9][0-9,]*"
                        + "(?:\\\\.[0-9]{1,2})?/g"
                        + ")||[];"

                        + "if(prices.length===0)"
                        + "continue;"

                        + "let price=prices[prices.length-1]||'';"
                        + "let mrp=prices.length>1?prices[0]:'';"

                        + "let lines=text.split(/\\\\n|\\\\r/)"
                        + ".map(x=>x.trim())"
                        + ".filter(Boolean);"

                        + "let name=lines[0]||'';"

                        + "if(/^(add|buy|quick|view|see|₹|rs)/i.test(name)"
                        + " && lines.length>1)"
                        + "name=lines[1];"

                        + "let img=el.querySelector('img');"

                        + "let image=img?"
                        + "(img.src||img.getAttribute('data-src')||'')"
                        + ":'';"

                        + "let a=el.querySelector('a');"
                        + "let link=a?a.href:location.href;"

                        + "let key=(name+'|'+mrp+'|'+price)"
                        + ".toLowerCase();"

                        + "if(seen.has(key)) continue;"
                        + "seen.add(key);"

                        + "output.push({"
                        + "description:name,"
                        + "brand:'',"
                        + "pack_size:'',"
                        + "mrp:mrp,"
                        + "price:price,"
                        + "discount:'',"
                        + "availability:'',"
                        + "image_url:image,"
                        + "product_url:link,"
                        + "raw_text:text,"
                        + "website:'" + selectedSite + "'"
                        + "});"

                        + "if(output.length>=1000) break;"

                        + "}"

                        + "return JSON.stringify(output);"

                        + "})()";

        webView.evaluateJavascript(
                javascript,
                value -> {

                    try {

                        String jsonString = value;

                        if (jsonString.startsWith("\"")
                                && jsonString.endsWith("\"")) {

                            jsonString =
                                    new JSONTokener(jsonString)
                                            .nextValue()
                                            .toString();
                        }

                        JSONArray array =
                                new JSONArray(jsonString);

                        for (int i = 0;
                             i < array.length();
                             i++) {

                            products.add(
                                    array.getJSONObject(i)
                            );
                        }

                        status.setText(
                                "Products found: "
                                        + products.size()
                        );

                        Toast.makeText(
                                this,
                                products.size()
                                        + " products collected",
                                Toast.LENGTH_SHORT
                        ).show();

                    } catch (Exception e) {

                        status.setText(
                                "Scan error: "
                                        + e.getMessage()
                        );

                        Toast.makeText(
                                this,
                                "Product data could not be read",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }


    private static final String CATALOG_DISCOVERY_V1 = "catalog_discovery_v1";

private void discoverCatalog() {

        if (webView == null) {
            Toast.makeText(
                    this,
                    "Open the website first",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        status.setText("Discovering complete catalogue...");

        webView.evaluateJavascript(
                "(function(){return document.documentElement.outerHTML;})()",
                htmlValue -> {

                    try {

                        String html = htmlValue;

                        if (html != null &&
                                html.length() >= 2 &&
                                html.startsWith("\"") &&
                                html.endsWith("\"")) {

                            html = new JSONTokener(html)
                                    .nextValue()
                                    .toString();
                        }

                        Python py = Python.getInstance();

                        PyObject parser =
                                py.getModule("catalog_source_parser");

                        PyObject result =
                                parser.callAttr(
                                        "discover_catalog",
                                        html,
                                        selectedSite
                                );

                        JSONObject data =
                                new JSONObject(result.toString());

                        JSONArray discovered =
                                data.optJSONArray("products");

                        JSONArray apiUrls =
                                data.optJSONArray("api_urls");

                        int count =
                                discovered == null
                                        ? 0
                                        : discovered.length();

                        int urls =
                                apiUrls == null
                                        ? 0
                                        : apiUrls.length();
                    
                    StringBuilder statusApiDebug = new StringBuilder();
                    if (apiUrls != null) {
                        for (int i = 0; i < apiUrls.length(); i++) {
                            statusApiDebug.append(apiUrls.optString(i)).append("\n");
                        }
                    }
                    status.setText("Sources: " + urls + "\n" + statusApiDebug.toString());


                        products.clear();

                        if (discovered != null) {

                            for (int i = 0;
                                 i < discovered.length();
                                 i++) {

                                JSONObject item =
                                        discovered.getJSONObject(i);

                                products.add(item);
                            }
                        }

                        StringBuilder apiDebug = new StringBuilder();
            if (apiUrls != null) {
                for (int i = 0; i < apiUrls.length(); i++) {
                    apiDebug.append(apiUrls.optString(i)).append("\n");
                }
            }

            status.setText(
                    "Catalogue discovered: "
                            + count
                            + " articles | "
                            + urls
                            + " data sources\n\n"
                            + "API SOURCES:\n"
                            + apiDebug.toString()
            );

                        Toast.makeText(
                                this,
                                count + " catalogue articles discovered",
                                Toast.LENGTH_LONG
                        ).show();

                    } catch (Exception e) {

                        status.setText(
                                "Catalogue discovery error: "
                                        + e.getMessage()
                        );

                        Toast.makeText(
                                this,
                                "Catalogue discovery failed",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void exportExcel() {

        if (products.size() == 0) {

            Toast.makeText(
                    this,
                    "Scan products first",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        try {

            JSONArray array = new JSONArray();

            for (JSONObject product : products) {

                product.put(
                        "website",
                        selectedSite
                );

                product.put(
                        "pincode",
                        pincode.getText().toString()
                );

                array.put(product);
            }

            String fileName =
                    "Ecommerce_Products_"
                            + selectedSite.replace(" ", "_")
                            + "_"
                            + System.currentTimeMillis()
                            + ".xlsx";

            File folder =
        getFilesDir();

            File file =
                    new File(folder, fileName);

            Python.getInstance()
                    .getModule("excel_export")
                    .callAttr(
                            "create_xlsx",
                            array.toString(),
                            file.getAbsolutePath()
                    );

            shareExcel(file);

        } catch (Exception e) {

            status.setText(
                    "Excel export error: "
                            + e.getMessage()
            );

            Toast.makeText(
                    this,
                    "Excel export failed",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void shareExcel(File file) {

        Uri uri =
                FileProvider.getUriForFile(
                        this,
                        getPackageName()
                                + ".fileprovider",
                        file
                );

        Intent intent =
                new Intent(Intent.ACTION_SEND);

        intent.setType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );

        intent.putExtra(
                Intent.EXTRA_STREAM,
                uri
        );

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
        );

        startActivity(
                Intent.createChooser(
                        intent,
                        "Save / Share Excel"
                )
        );

        status.setText(
                "Excel created successfully."
        );
    }

    @Override
    public void onBackPressed() {

        if (webView != null
                && webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }
}
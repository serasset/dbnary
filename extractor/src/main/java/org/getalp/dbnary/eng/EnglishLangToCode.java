/**
 *
 */
package org.getalp.dbnary.eng;

import org.getalp.dbnary.LangTools;
import org.getalp.iso639.ISO639_3;

import java.io.*;
import java.util.HashMap;

/**
 * @author Mariam
 */
public class EnglishLangToCode extends LangTools {
    static HashMap<String, String> h = new HashMap<String, String>();

    static {
        add("Abkhaz", "ab");
        add("Acehnese", "ace");
        add("Acholi", "ach");
        add("Adangme", "ada");
        add("Adyghe", "ady");
        add("Afar", "aa");
        add("Afrihili", "afh");
        add("Afrikaans", "af");
        add("Ainu", "ain");
        add("Akan", "ak");
        add("Akkadian", "akk");
        add("Albanian", "sq");
        add("Alemannic German", "gsw");
        add("Aleut", "ale");
        add("Amharic", "am");
        add("Ancient Greek", "grc");
        add("Angika", "anp");
        add("Arabic", "ar");
        add("Aragonese", "an");
        add("Aramaic", "arc");
        add("Arapaho", "arp");
        add("Arawak", "arw");
        add("Armenian", "hy");
        add("Aromanian", "rup");
        add("Assamese", "as");
        add("Asturian", "ast");
        add("Avar", "av");
        add("Avestan", "ae");
        add("Awadhi", "awa");
        add("Aymara", "ay");
        add("Azeri", "az");
        add("Balinese", "ban");
        add("Baluchi", "bal");
        add("Bambara", "bm");
        add("Basaa", "bas");
        add("Bashkir", "ba");
        add("Basque", "eu");
        add("Beja", "bej");
        add("Belarusian", "be");
        add("Bemba", "bem");
        add("Bengali", "bn");
        add("Bhojpuri", "bho");
        add("Bikol", "bik");
        add("Bini", "bin");
        add("Bislama", "bi");
        add("Blackfoot", "bla");
        add("Blin", "byn");
        add("Bosnian", "bs");
        add("Braj", "bra");
        add("Breton", "br");
        add("Buginese", "bug");
        add("Bulgarian", "bg");
        add("Burmese", "my");
        add("Buryat", "bua");
        add("Caddo", "cad");
        add("Catalan", "ca");
        add("Cebuano", "ceb");
        add("Chagatai", "chg");
        add("Chamorro", "ch");
        add("Chechen", "ce");
        add("Cherokee", "chr");
        add("Cheyenne", "chy");
        add("Chibcha", "chb");
        add("Chichewa", "ny");
        add("Chinook Jargon", "chn");
        add("Chipewyan", "chp");
        add("Choctaw", "cho");
        add("Chuukese", "chk");
        add("Chuvash", "cv");
        add("Classical Newari", "nwc");
        add("Classical Syriac", "syc");
        add("Coptic", "cop");
        add("Cornish", "kw");
        add("Corsican", "co");
        add("Cree", "cr");
        add("Creek", "mus");
        add("Crimean Tatar", "crh");
        add("Croatian", "hr");
        add("Czech", "cs");
        add("Dakota", "dak");
        add("Danish", "da");
        add("Dargwa", "dar");
        add("Dhivehi", "dv");
        add("Dinka", "din");
        add("Dogri", "doi");
        add("Dogrib", "dgr");
        add("Duala", "dua");
        add("Dutch", "nl");
        add("Dyula", "dyu");
        add("Dzongkha", "dz");
        add("Eastern Frisian", "frs");
        add("Efik", "efi");
        add("Egyptian", "egy");
        add("Ekajuk", "eka");
        add("Elamite", "elx");
        add("English", "en");
        add("Erzya", "myv");
        add("Esperanto", "eo");
        add("Estonian", "et");
        add("Ewe", "ee");
        add("Ewondo", "ewo");
        add("Fanti", "fat");
        add("Faroese", "fo");
        add("Fijian", "fj");
        add("Finnish", "fi");
        add("Fon", "fon");
        add("French", "fr");
        add("Friulian", "fur");
        add("Fula", "ff");
        add("Ga", "gaa");
        add("Galibi Carib", "car");
        add("Galician", "gl");
        add("Gayo", "gay");
        add("Gbaya", "gba");
        add("Ge'ez", "gez");
        add("Georgian", "ka");
        add("German", "de");
        add("Gilbertese", "gil");
        add("Gondi", "gon");
        add("Gorontalo", "gor");
        add("Gothic", "got");
        add("Grebo", "grb");
        add("Greek", "el");
        add("Greenlandic", "kl");
        add("Guaraní", "gn");
        add("Gujarati", "gu");
        add("Gwich'in", "gwi");
        add("Haida", "hai");
        add("Haitian Creole", "ht");
        add("Hausa", "ha");
        add("Hawaiian", "haw");
        add("Hebrew", "he");
        add("Herero", "hz");
        add("Hiligaynon", "hil");
        add("Hindi", "hi");
        add("Hiri Motu", "ho");
        add("Hittite", "hit");
        add("Hmong", "hmn");
        add("Hungarian", "hu");
        add("Hupa", "hup");
        add("Iban", "iba");
        add("Icelandic", "is");
        add("Ido", "io");
        add("Igbo", "ig");
        add("Ilocano", "ilo");
        add("Inari Sami", "smn");
        add("Indonesian", "id");
        add("Ingush", "inh");
        add("Interlingua", "ia");
        add("Interlingue", "ie");
        add("Inuktitut", "iu");
        add("Inupiak", "ik");
        add("Irish", "ga");
        add("Italian", "it");
        add("Japanese", "ja");
        add("Javanese", "jv");
        add("Jingpho", "kac");
        add("Judeo-Arabic", "jrb");
        add("Judeo-Persian", "jpr");
        add("Kabardian", "kbd");
        add("Kabyle", "kab");
        add("Kalmyk", "xal");
        add("Kamba", "kam");
        add("Kannada", "kn");
        add("Kanuri", "kr");
        add("Kapampangan", "pam");
        add("Karachay-Balkar", "krc");
        add("Karakalpak", "kaa");
        add("Karelian", "krl");
        add("Kashmiri", "ks");
        add("Kashubian", "csb");
        add("Kazakh", "kk");
        add("Khasi", "kha");
        add("Khmer", "km");
        add("Khotanese", "kho");
        add("Kikuyu", "ki");
        add("Kimbundu", "kmb");
        add("Kinyarwanda", "rw");
        add("Kirundi", "rn");
        add("Komi-Zyrian", "kv");
        add("Kongo", "kg");
        add("Konkani", "kok");
        add("Korean", "ko");
        add("Kosraean", "kos");
        add("Kpelle", "kpe");
        add("Kumyk", "kum");
        add("Kurdish", "ku");
        add("Kurukh", "kru");
        add("Kutenai", "kut");
        add("Kwanyama", "kj");
        add("Kyrgyz", "ky");
        add("Ladino", "lad");
        add("Lahnda", "lah");
        add("Lamba", "lam");
        add("Lao", "lo");
        add("Latin", "la");
        add("Latvian", "lv");
        add("Lenape", "del");
        add("Lezgi", "lez");
        add("Limburgish", "li");
        add("Lingala", "ln");
        add("Lithuanian", "lt");
        add("Lojban", "jbo");
        add("Low German", "nds");
        add("Lower Sorbian", "dsb");
        add("Lozi", "loz");
        add("Luba-Katanga", "lu");
        add("Luganda", "lg");
        add("Luiseno", "lui");
        add("Lule Sami", "smj");
        add("Lunda", "lun");
        add("Luo", "luo");
        add("Luxembourgish", "lb");
        add("Maasai", "mas");
        add("Macedonian", "mk");
        add("Madurese", "mad");
        add("Magahi", "mag");
        add("Maithili", "mai");
        add("Makasar", "mak");
        add("Malagasy", "mg");
        add("Malay", "ms");
        add("Malayalam", "ml");
        add("Maltese", "mt");
        add("Manchu", "mnc");
        add("Mandar", "mdr");
        add("Mandarin", "zh");
        add("Mandingo", "man");
        add("Manipuri", "mni");
        add("Manx", "gv");
        add("Maori", "mi");
        add("Mapudungun", "arn");
        add("Marathi", "mr");
        add("Mari", "chm");
        add("Marshallese", "mh");
        add("Marwari", "mwr");
        add("Mende", "men");
        add("Middle Dutch", "dum");
        add("Middle English", "enm");
        add("Middle French", "frm");
        add("Middle High German", "gmh");
        add("Middle Irish", "mga");
        add("Middle Persian", "pal");
        add("Mi'kmaq", "mic");
        add("Minangkabau", "min");
        add("Mirandese", "mwl");
        add("Mizo", "lus");
        add("Mohawk", "moh");
        add("Moksha", "mdf");
        add("Mongo", "lol");
        add("Mongolian", "mn");
        add("More", "mos");
        add("Nauruan", "na");
        add("Navajo", "nv");
        add("Ndonga", "ng");
        add("Neapolitan", "nap");
        add("Nepali", "ne");
        add("Newari", "new");
        add("Nias", "nia");
        add("Niuean", "niu");
        add("N'Ko", "nqo");
        add("Nogai", "nog");
        add("North Frisian", "frr");
        add("Northern Ndebele", "nd");
        add("Northern Sami", "se");
        add("Northern Sotho", "nso");
        add("Norwegian", "no");
        add("Norwegian Bokmål", "nb");
        add("Norwegian Nynorsk", "nn");
        add("Nyamwezi", "nym");
        add("Nyankole", "nyn");
        add("Nyoro", "nyo");
        add("Nzima", "nzi");
        add("Occitan", "oc");
        add("Ojibwe", "oj");
        add("Old Church Slavonic", "cu");
        add("Old English", "ang");
        add("Old French", "fro");
        add("Old High German", "goh");
        add("Old Irish", "sga");
        add("Old Javanese", "kaw");
        add("Old Norse", "non");
        add("Old Provençal", "pro");
        add("Old Persian", "peo");
        add("Oriya", "or");
        add("Oromo", "om");
        add("Osage", "osa");
        add("Ossetian", "os");
        add("Ottoman Turkish", "ota");
        add("Pahouin", "fan");
        add("Palauan", "pau");
        add("Pali", "pi");
        add("Pangasinan", "pag");
        add("Papiamentu", "pap");
        add("Pashto", "ps");
        add("Persian", "fa");
        add("Phoenician", "phn");
        add("Pohnpeian", "pon");
        add("Polish", "pl");
        add("Portuguese", "pt");
        add("Punjabi", "pa");
        add("Quechua", "qu");
        add("Rajasthani", "raj");
        add("Rapa Nui", "rap");
        add("Rarotongan", "rar");
        add("Romani", "rom");
        add("Romanian", "ro");
        add("Romansch", "rm");
        add("Russian", "ru");
        add("Samaritan Aramaic", "sam");
        add("Samoan", "sm");
        add("Sandawe", "sad");
        add("Sango", "sg");
        add("Sanskrit", "sa");
        add("Santali", "sat");
        add("Sardinian", "sc");
        add("Sasak", "sas");
        add("Scots", "sco");
        add("Scottish Gaelic", "gd");
        add("Selkup", "sel");
        add("Serbian", "sr");
        add("Serbo-Croatian", "sh");
        add("Serer", "srr");
        add("Shan", "shn");
        add("Shona", "sn");
        add("Sichuan Yi", "ii");
        add("Sicilian", "scn");
        add("Sidamo", "sid");
        add("Sindhi", "sd");
        add("Sinhalese", "si");
        add("Siska", "tog");
        add("Skolt Sami", "sms");
        add("Slavey", "den");
        add("Slovak", "sk");
        add("Slovene", "sl");
        add("Sogdian", "sog");
        add("Somali", "so");
        add("Soninke", "snk");
        add("Sotho", "st");
        add("Southern Altai", "alt");
        add("Southern Ndebele", "nr");
        add("Southern Sami", "sma");
        add("Spanish", "es");
        add("Sranan Tongo", "srn");
        add("Sukuma", "suk");
        add("Sumerian", "sux");
        add("Sundanese", "su");
        add("Susu", "sus");
        add("Swahili", "sw");
        add("Swati", "ss");
        add("Swedish", "sv");
        add("Syriac", "syr");
        add("Tagalog", "tl");
        add("Tahitian", "ty");
        add("Tajik", "tg");
        add("Tamashek", "tmh");
        add("Tamil", "ta");
        add("Tatar", "tt");
        add("Telugu", "te");
        add("Template:fil", "fil");
        add("Template:mis", "mis");
        add("Template:mo", "mo");
        add("Template:tlh", "tlh");
        add("Template:zbl", "zbl");
        add("Template:zxx", "zxx");
        add("Tereno", "ter");
        add("Tetum", "tet");
        add("Thai", "th");
        add("Tibetan", "bo");
        add("Tigre", "tig");
        add("Tigrinya", "ti");
        add("Timne", "tem");
        add("Tivi", "tiv");
        add("Tlingit", "tli");
        add("Tok Pisin", "tpi");
        add("Tokelauan", "tkl");
        add("Tongan", "to");
        add("Translingual", "mul");
        add("Tshiluba", "lua");
        add("Tsimshian", "tsi");
        add("Tsonga", "ts");
        add("Tswana", "tn");
        add("Tumbuka", "tum");
        add("Turkish", "tr");
        add("Turkmen", "tk");
        add("Tuvaluan", "tvl");
        add("Tuvan", "tyv");
        add("Twi", "tw");
        add("Udmurt", "udm");
        add("Ugaritic", "uga");
        add("Ukrainian", "uk");
        add("Umbundu", "umb");
        add("Undetermined", "und");
        add("Upper Sorbian", "hsb");
        add("Urdu", "ur");
        add("Uyghur", "ug");
        add("Uzbek", "uz");
        add("Vai", "vai");
        add("Venda", "ve");
        add("Vietnamese", "vi");
        add("Volapük", "vo");
        add("Votic", "vot");
        add("Walamo", "wal");
        add("Walloon", "wa");
        add("Waray-Waray", "war");
        add("Washo", "was");
        add("Welsh", "cy");
        add("West Frisian", "fy");
        add("Wolof", "wo");
        add("Xhosa", "xh");
        add("Yakut", "sah");
        add("Yao", "yao");
        add("Yapese", "yap");
        add("Yiddish", "yi");
        add("Yoruba", "yo");
        add("Zapotec", "zap");
        add("Zazaki", "zza");
        add("Zenaga", "zen");
        add("Zhuang", "za");
        add("Zulu", "zu");
        add("Zuni", "zun");

        // uncommon languages
        add("Abhiri", "abh-prk");
        add("Abhiri Prakrit", "abh-prk");
        add("Acadian French", "fr-aca");
        add("Addu Dhivehi", "add-dv");
        add("Addu Divehi", "add-dv");
        add("Addu Bas", "add-dv");
        add("Aeolic Greek", "el-aeo");
        add("Lesbic Greek", "el-aeo");
        add("Lesbian Greek", "el-aeo");
        add("Aeolian Greek", "el-aeo");
        add("American English", "en-US");
        add("Amoy", "nan-amo");
        add("Xiamenese", "nan-amo");
        add("Arcadian Greek", "el-arc");
        add("Arcadocypriot Greek", "el-arp");
        add("Attic Greek", "el-att");
        add("Austrian German", "de-AT");
        add("Avanti", "prk-avt");
        add("Avanti Prakrit", "prk-avt");
        add("Bahliki", "bhl-prk");
        add("Bahliki Prakrit", "bhl-prk");
        add("Bombay Hindi", "hi-mum");
        add("Mumbai Hindi", "hi-mum");
        add("Bambai Hindi", "hi-mum");
        add("British English", "en-GB");
        add("Byzantine Greek", "gkm Medieval Greek");
        add("Medieval Greek", "gkm Medieval Greek");
        add("Cajun French", "frc");
        add("Louisiana French", "frc");
        add("Canadian French", "fr-CA");
        add("Candali", "cnd-prk");
        add("Candali Prakrit", "cnd-prk");
        add("Classical Tagalog", "tl-cls");
        add("Cretan Greek", "el-crt");
        add("Cypriotic Greek", "el-cyp");
        add("Daksinatya", "dks-prk");
        add("Daksinatya Prakrit", "dks-prk");
        add("Doric Greek", "el-dor");
        add("Dramili", "drm-prk");
        add("Dramili Prakrit", "drm-prk");
        add("Early Scots", "sco-osc");
        add("Old Scots", "sco-osc");
        add("O.Sc.", "sco-osc");
        add("Ecclesiastical Latin", "la-ecc");
        add("Church Latin", "la-ecc");
        add("EL.", "la-ecc");
        add("Elean Greek", "el-ela");
        add("Epic Greek", "el-epc");
        add("Griko", "el-grk");
        add("Grico", "el-grk");
        add("Guernésiais", "roa-grn");
        add("Hainanese", "nan-hai");
        add("Helu", "elu-prk");
        add("Hela", "elu-prk");
        add("Elu Prakrit", "elu-prk");
        add("Helu Prakrit", "elu-prk");
        add("Hela Prakrit", "elu-prk");
        add("Hokkien", "nan-hok");
        add("Homeric Greek", "el-hmr");
        add("Huvadhu Dhivehi", "hvd-dv");
        add("Huvadhu Divehi", "hvd-dv");
        add("Huvadhu Bas", "hvd-dv");
        add("Insular Scots", "sco-ins");
        add("Ins.Sc.", "sco-ins");
        add("Ionic Greek", "el-ion");
        add("Jèrriais", "roa-jer");
        add("Jewish Aramaic", "sem-jar");
        add("Kathiyawadi", "gu-kat");
        add("Kathiyawadi Gujarati", "gu-kat");
        add("Kathiawadi", "gu-kat");
        add("Koine Greek", "grc-koi Koine");
        add("Kromanti", "alv-kro");
        add("Late Latin", "la-lat");
        add("LL", "la-lat");
        add("LL.", "la-lat");
        add("Lunfardo", "es-lun Lunfardo");
        add("Medieval Latin", "la-med");
        add("ML", "la-med");
        add("ML.", "la-med");
        add("Medieval Sinhalese", "si-med");
        add("Medieval Sinhala", "si-med");
        add("Mercian Old English", "ang-mer");
        add("Middle Bengali", "bn-mid");
        add("Middle Gujarati", "gu-mid");
        add("Middle Iranian", "ira-mid ");
        add("MIr.", "ira-mid ");
        add("Middle Kannada", "kn-mid");
        add("Middle Konkani", "kok-mid");
        add("Medieval Konkani", "kok-mid");
        add("Middle Oriya", "or-mid");
        add("Middle Scots", "sco-smi");
        add("Mid.Sc.", "sco-smi");
        add("Middle Tamil", "ta-mid");
        add("Modern Greek", "el-GR ell");
        add("Modern Israeli Hebrew", "he-IL");
        add("Mulaku Dhivehi", "mlk-dv");
        add("Mulaku Divehi", "mlk-dv");
        add("Mulaku Bas", "mlk-dv");
        add("New Latin", "la-new");
        add("Modern Latin", "la-new");
        add("NL.", "la-new");
        add("Northern Scots", "sco-nor");
        add("Nor.Sc.", "sco-nor");
        add("Northumbrian Old English", "ang-nor");
        add("Odri", "odr-prk");
        add("Odri Prakrit", "odr-prk");
        add("Old Bengali", "bn-old");
        add("Old Gujarati", "gu-old");
        add("Old Hindi", "hi-old");
        add("Old Iranian", "ira-old");
        add("OIr.", "ira-old");
        add("Old Kannada", "kn-old");
        add("Old Konkani", "kok-old");
        add("Early Konkani", "kok-old");
        add("Old Northern French", "fro-nor");
        add("Old Norman", "fro-nor");
        add("Old Norman French", "fro-nor");
        add("ONF", "fro-nor");
        add("Old Oriya", "or-old");
        add("Old Picard", "fro-pic");
        add("Old Punjabi", "pa-old");
        add("Old Tagalog", "tl-old");
        add("Old Xiang", "hsn-old");
        add("Lou-Shao", "hsn-old");
        add("Opuntian Locrian", "loc-opu");
        add("Ozolian Locrian", "loc-ozo");
        add("Paisaci", "psc-prk");
        add("Paisaci Prakrit", "psc-prk");
        add("Pamphylian Greek", "el-pam");
        add("Paphian Greek", "el-pap");
        add("Philippine Hokkien", "nan-phl");
        add("Pinghua", "pinhua");
        add("Pracya", "prc-prk");
        add("Pracya Prakrit", "prc-prk");
        add("Pre-Greek", "qfa-sub-grc");
        add("Pre-Greek", "pregrc");
        add("pre-Roman (Balkans)", "und-bal");
        add("pre-Roman (Iberia)", "und-ibe");
        add("Proto-Baltic", "bat-pro");
        add("Proto-Canaanite", "sem-can-pro");
        add("Proto-Finno-Permic", "fiu-fpr-pro");
        add("Proto-Finno-Ugric", "fiu-pro");
        add("Provençal", "prv");
        add("Renaissance Latin", "la-ren");
        add("RL.", "la-ren");
        add("Sabari", "sbr-prk");
        add("Sabari Prakrit", "sbr-prk");
        add("Shanghainese", "wuu-sha");
        add("Sha.", "wuu-sha");
        add("Southern Scots", "sco-sou");
        add("Borders Scots", "sco-sou");
        add("Sou.Sc.", "sco-sou");
        add("Suevic", "gem-sue");
        add("Suebian", "gem-sue");
        add("Taishanese", "yue-tai");
        add("Teochew", "nan-teo");
        add("Thessalian Greek", "el-ths");
        add("Transalpine Gaulish", "xtg");
        add("Ulster Scots", "sco-uls");
        add("Uls.Sc.", "sco-uls");
        add("Viennese German", "de-AT-vie");
        add("VG.", "de-AT-vie");
        add("Vulgar Latin", "la-vul");
        add("VL.", "la-vul");
        add("Wuhua Chinese", "hak-wuh");

        add("Ammonite", "ammonite");


        InputStream fis = null;
        try {
            fis = EnglishLangToCode.class.getResourceAsStream("data3.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));

            String s = br.readLine();
            while (s != null) {
                String[] line = s.split("\t");
                if (line.length >= 2) {
                    String code = line[0];
                    for (int i = 1; i < line.length; i++) {
                        add(line[i], code);
                    }
                } else {
                    // System.err.println("Unrecognized line:" + s);
                }
                s = br.readLine();
            }


        } catch (UnsupportedEncodingException e) {
            // This should really never happen
        } catch (IOException e) {
            // don't know what I should do here, as the data should be bundled with the code.
            e.printStackTrace();
        } finally {
            if (fis != null)
                try {
                    fis.close();
                } catch (IOException e) {
                    // nop
                }
        }

        for (ISO639_3.Lang l : ISO639_3.sharedInstance.knownLanguages()) {
            add(l.getEn(), l.getId());
        }


    }

    public static String threeLettersCode(String s) {
        return threeLettersCode(h, s);
    }

    private static void add(String n, String c) {
        h.put(n.toLowerCase(), c);
    }
}

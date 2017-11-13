package org.getalp.dbnary.nld;

import java.util.HashMap;
import org.getalp.LangTools;

/**
 * @author malick
 */
public class NetherlandLangToCode extends LangTools {

  static HashMap<String, String> h = new HashMap<String, String>();

  static {
    h.put("Abchazische", "ab");
    h.put("acoli", "ace");
    h.put("Acholi", "ach");
    h.put("Adangme", "ada");
    h.put("Adyghe", "ady");
    h.put("Afar", "aa");
    h.put("Afrihili", "afh");
    h.put("Afrikaans", "af");
    h.put("Ainu", "ain");
    h.put("Akan", "ak");
    h.put("Akkadian", "akk");
    h.put("Albanian", "sq");
    h.put("Alemannic German", "gsw");
    h.put("Aleut", "ale");
    h.put("Amharic", "am");
    h.put("Ancient Greek", "grc");
    h.put("Angika", "anp");
    h.put("Arabic", "ar");
    h.put("Aragonese", "an");
    h.put("Aramaic", "arc");
    h.put("Arapaho", "arp");
    h.put("Arawak", "arw");
    h.put("Armenian", "hy");
    h.put("Aromanian", "rup");
    h.put("Assamese", "as");
    h.put("Asturian", "ast");
    h.put("Avar", "av");
    h.put("Avestan", "ae");
    h.put("Awadhi", "awa");
    h.put("Aymara", "ay");
    h.put("Azeri", "az");
    h.put("Balinese", "ban");
    h.put("Baluchi", "bal");
    h.put("Bambara", "bm");
    h.put("Basaa", "bas");
    h.put("Bashkir", "ba");
    h.put("Basque", "eu");
    h.put("Beja", "bej");
    h.put("Belarusian", "be");
    h.put("Bemba", "bem");
    h.put("Bengali", "bn");
    h.put("Bhojpuri", "bho");
    h.put("Bikol", "bik");
    h.put("Bini", "bin");
    h.put("Bislama", "bi");
    h.put("Blackfoot", "bla");
    h.put("Blin", "byn");
    h.put("Bosnian", "bs");
    h.put("Braj", "bra");
    h.put("Breton", "br");
    h.put("Buginese", "bug");
    h.put("Bulgarian", "bg");
    h.put("Burmese", "my");
    h.put("Buryat", "bua");
    h.put("Caddo", "cad");
    h.put("Catalan", "ca");
    h.put("Cebuano", "ceb");
    h.put("Chagatai", "chg");
    h.put("Chamorro", "ch");
    h.put("Chechen", "ce");
    h.put("Cherokee", "chr");
    h.put("Cheyenne", "chy");
    h.put("Chibcha", "chb");
    h.put("Chichewa", "ny");
    h.put("Chinook Jargon", "chn");
    h.put("Chipewyan", "chp");
    h.put("Choctaw", "cho");
    h.put("Chuukese", "chk");
    h.put("Chuvash", "cv");
    h.put("Classical Newari", "nwc");
    h.put("Classical Syriac", "syc");
    h.put("Coptic", "cop");
    h.put("Cornish", "kw");
    h.put("Corsican", "co");
    h.put("Cree", "cr");
    h.put("Creek", "mus");
    h.put("Crimean Tatar", "crh");
    h.put("Croatian", "hr");
    h.put("Czech", "cs");
    h.put("Dakota", "dak");
    h.put("Danish", "da");
    h.put("Dargwa", "dar");
    h.put("Dhivehi", "dv");
    h.put("Dinka", "din");
    h.put("Dogri", "doi");
    h.put("Dogrib", "dgr");
    h.put("Duala", "dua");
    h.put("Dutch", "nl");
    h.put("Dyula", "dyu");
    h.put("Dzongkha", "dz");
    h.put("Eastern Frisian", "frs");
    h.put("Efik", "efi");
    h.put("Egyptian", "egy");
    h.put("Ekajuk", "eka");
    h.put("Elamite", "elx");
    h.put("English", "en");
    h.put("Erzya", "myv");
    h.put("Esperanto", "eo");
    h.put("Estonian", "et");
    h.put("Ewe", "ee");
    h.put("Ewondo", "ewo");
    h.put("Fanti", "fat");
    h.put("Faroese", "fo");
    h.put("Fijian", "fj");
    h.put("Finnish", "fi");
    h.put("Fon", "fon");
    h.put("French", "fr");
    h.put("Friulian", "fur");
    h.put("Fula", "ff");
    h.put("Ga", "gaa");
    h.put("Galibi Carib", "car");
    h.put("Galician", "gl");
    h.put("Gayo", "gay");
    h.put("Gbaya", "gba");
    h.put("Ge'ez", "gez");
    h.put("Georgian", "ka");
    h.put("German", "de");
    h.put("Gilbertese", "gil");
    h.put("Gondi", "gon");
    h.put("Gorontalo", "gor");
    h.put("Gothic", "got");
    h.put("Grebo", "grb");
    h.put("Greek", "el");
    h.put("Greenlandic", "kl");
    h.put("Guaraní", "gn");
    h.put("Gujarati", "gu");
    h.put("Gwich'in", "gwi");
    h.put("Haida", "hai");
    h.put("Haitian Creole", "ht");
    h.put("Hausa", "ha");
    h.put("Hawaiian", "haw");
    h.put("Hebrew", "he");
    h.put("Herero", "hz");
    h.put("Hiligaynon", "hil");
    h.put("Hindi", "hi");
    h.put("Hiri Motu", "ho");
    h.put("Hittite", "hit");
    h.put("Hmong", "hmn");
    h.put("Hungarian", "hu");
    h.put("Hupa", "hup");
    h.put("Iban", "iba");
    h.put("Icelandic", "is");
    h.put("Ido", "io");
    h.put("Igbo", "ig");
    h.put("Ilocano", "ilo");
    h.put("Inari Sami", "smn");
    h.put("Indonesian", "id");
    h.put("Ingush", "inh");
    h.put("Interlingua", "ia");
    h.put("Interlingue", "ie");
    h.put("Inuktitut", "iu");
    h.put("Inupiak", "ik");
    h.put("Irish", "ga");
    h.put("Italian", "it");
    h.put("Japanese", "ja");
    h.put("Javanese", "jv");
    h.put("Jingpho", "kac");
    h.put("Judeo-Arabic", "jrb");
    h.put("Judeo-Persian", "jpr");
    h.put("Kabardian", "kbd");
    h.put("Kabyle", "kab");
    h.put("Kalmyk", "xal");
    h.put("Kamba", "kam");
    h.put("Kannada", "kn");
    h.put("Kanuri", "kr");
    h.put("Kapampangan", "pam");
    h.put("Karachay-Balkar", "krc");
    h.put("Karakalpak", "kaa");
    h.put("Karelian", "krl");
    h.put("Kashmiri", "ks");
    h.put("Kashubian", "csb");
    h.put("Kazakh", "kk");
    h.put("Khasi", "kha");
    h.put("Khmer", "km");
    h.put("Khotanese", "kho");
    h.put("Kikuyu", "ki");
    h.put("Kimbundu", "kmb");
    h.put("Kinyarwanda", "rw");
    h.put("Kirundi", "rn");
    h.put("Komi-Zyrian", "kv");
    h.put("Kongo", "kg");
    h.put("Konkani", "kok");
    h.put("Korean", "ko");
    h.put("Kosraean", "kos");
    h.put("Kpelle", "kpe");
    h.put("Kumyk", "kum");
    h.put("Kurdish", "ku");
    h.put("Kurukh", "kru");
    h.put("Kutenai", "kut");
    h.put("Kwanyama", "kj");
    h.put("Kyrgyz", "ky");
    h.put("Ladino", "lad");
    h.put("Lahnda", "lah");
    h.put("Lamba", "lam");
    h.put("Lao", "lo");
    h.put("Latin", "la");
    h.put("Latvian", "lv");
    h.put("Lenape", "del");
    h.put("Lezgi", "lez");
    h.put("Limburgish", "li");
    h.put("Lingala", "ln");
    h.put("Lithuanian", "lt");
    h.put("Lojban", "jbo");
    h.put("Low German", "nds");
    h.put("Lower Sorbian", "dsb");
    h.put("Lozi", "loz");
    h.put("Luba-Katanga", "lu");
    h.put("Luganda", "lg");
    h.put("Luiseno", "lui");
    h.put("Lule Sami", "smj");
    h.put("Lunda", "lun");
    h.put("Luo", "luo");
    h.put("Luxembourgish", "lb");
    h.put("Maasai", "mas");
    h.put("Macedonian", "mk");
    h.put("Madurese", "mad");
    h.put("Magahi", "mag");
    h.put("Maithili", "mai");
    h.put("Makasar", "mak");
    h.put("Malagasy", "mg");
    h.put("Malay", "ms");
    h.put("Malayalam", "ml");
    h.put("Maltese", "mt");
    h.put("Manchu", "mnc");
    h.put("Mandar", "mdr");
    h.put("Mandarin", "zh");
    h.put("Mandingo", "man");
    h.put("Manipuri", "mni");
    h.put("Manx", "gv");
    h.put("Maori", "mi");
    h.put("Mapudungun", "arn");
    h.put("Marathi", "mr");
    h.put("Mari", "chm");
    h.put("Marshallese", "mh");
    h.put("Marwari", "mwr");
    h.put("Mende", "men");
    h.put("Middle Dutch", "dum");
    h.put("Middle English", "enm");
    h.put("Middle French", "frm");
    h.put("Middle High German", "gmh");
    h.put("Middle Irish", "mga");
    h.put("Middle Persian", "pal");
    h.put("Mi'kmaq", "mic");
    h.put("Minangkabau", "min");
    h.put("Mirandese", "mwl");
    h.put("Mizo", "lus");
    h.put("Mohawk", "moh");
    h.put("Moksha", "mdf");
    h.put("Mongo", "lol");
    h.put("Mongolian", "mn");
    h.put("More", "mos");
    h.put("Nauruan", "na");
    h.put("Navajo", "nv");
    h.put("Ndonga", "ng");
    h.put("Neapolitan", "nap");
    h.put("Nepali", "ne");
    h.put("Newari", "new");
    h.put("Nias", "nia");
    h.put("Niuean", "niu");
    h.put("N'Ko", "nqo");
    h.put("Nogai", "nog");
    h.put("North Frisian", "frr");
    h.put("Northern Ndebele", "nd");
    h.put("Northern Sami", "se");
    h.put("Northern Sotho", "nso");
    h.put("Norwegian", "no");
    h.put("Norwegian Bokmål", "nb");
    h.put("Norwegian Nynorsk", "nn");
    h.put("Nyamwezi", "nym");
    h.put("Nyankole", "nyn");
    h.put("Nyoro", "nyo");
    h.put("Nzima", "nzi");
    h.put("Occitan", "oc");
    h.put("Ojibwe", "oj");
    h.put("Old Church Slavonic", "cu");
    h.put("Old English", "ang");
    h.put("Old French", "fro");
    h.put("Old High German", "goh");
    h.put("Old Irish", "sga");
    h.put("Old Javanese", "kaw");
    h.put("Old Norse", "non");
    h.put("Old Provençal", "pro");
    h.put("Old Persian", "peo");
    h.put("Oriya", "or");
    h.put("Oromo", "om");
    h.put("Osage", "osa");
    h.put("Ossetian", "os");
    h.put("Ottoman Turkish", "ota");
    h.put("Pahouin", "fan");
    h.put("Palauan", "pau");
    h.put("Pali", "pi");
    h.put("Pangasinan", "pag");
    h.put("Papiamentu", "pap");
    h.put("Pashto", "ps");
    h.put("Persian", "fa");
    h.put("Phoenician", "phn");
    h.put("Pohnpeian", "pon");
    h.put("Polish", "pl");
    h.put("Portuguese", "pt");
    h.put("Punjabi", "pa");
    h.put("Quechua", "qu");
    h.put("Rajasthani", "raj");
    h.put("Rapa Nui", "rap");
    h.put("Rarotongan", "rar");
    h.put("Romani", "rom");
    h.put("Romanian", "ro");
    h.put("Romansch", "rm");
    h.put("Russian", "ru");
    h.put("Samaritan Aramaic", "sam");
    h.put("Samoan", "sm");
    h.put("Sandawe", "sad");
    h.put("Sango", "sg");
    h.put("Sanskrit", "sa");
    h.put("Santali", "sat");
    h.put("Sardinian", "sc");
    h.put("Sasak", "sas");
    h.put("Scots", "sco");
    h.put("Scottish Gaelic", "gd");
    h.put("Selkup", "sel");
    h.put("Serbian", "sr");
    h.put("Serbo-Croatian", "sh");
    h.put("Serer", "srr");
    h.put("Shan", "shn");
    h.put("Shona", "sn");
    h.put("Sichuan Yi", "ii");
    h.put("Sicilian", "scn");
    h.put("Sidamo", "sid");
    h.put("Sindhi", "sd");
    h.put("Sinhalese", "si");
    h.put("Siska", "tog");
    h.put("Skolt Sami", "sms");
    h.put("Slavey", "den");
    h.put("Slovak", "sk");
    h.put("Slovene", "sl");
    h.put("Sogdian", "sog");
    h.put("Somali", "so");
    h.put("Soninke", "snk");
    h.put("Sotho", "st");
    h.put("Southern Altai", "alt");
    h.put("Southern Ndebele", "nr");
    h.put("Southern Sami", "sma");
    h.put("Spanish", "es");
    h.put("Sranan Tongo", "srn");
    h.put("Sukuma", "suk");
    h.put("Sumerian", "sux");
    h.put("Sundanese", "su");
    h.put("Susu", "sus");
    h.put("Swahili", "sw");
    h.put("Swati", "ss");
    h.put("Swedish", "sv");
    h.put("Syriac", "syr");
    h.put("Tagalog", "tl");
    h.put("Tahitian", "ty");
    h.put("Tajik", "tg");
    h.put("Tamashek", "tmh");
    h.put("Tamil", "ta");
    h.put("Tatar", "tt");
    h.put("Telugu", "te");
    h.put("Template:fil", "fil");
    h.put("Template:mis", "mis");
    h.put("Template:mo", "mo");
    h.put("Template:tlh", "tlh");
    h.put("Template:zbl", "zbl");
    h.put("Template:zxx", "zxx");
    h.put("Tereno", "ter");
    h.put("Tetum", "tet");
    h.put("Thai", "th");
    h.put("Tibetan", "bo");
    h.put("Tigre", "tig");
    h.put("Tigrinya", "ti");
    h.put("Timne", "tem");
    h.put("Tivi", "tiv");
    h.put("Tlingit", "tli");
    h.put("Tok Pisin", "tpi");
    h.put("Tokelauan", "tkl");
    h.put("Tongan", "to");
    h.put("Translingual", "mul");
    h.put("Tshiluba", "lua");
    h.put("Tsimshian", "tsi");
    h.put("Tsonga", "ts");
    h.put("Tswana", "tn");
    h.put("Tumbuka", "tum");
    h.put("Turkish", "tr");
    h.put("Turkmen", "tk");
    h.put("Tuvaluan", "tvl");
    h.put("Tuvan", "tyv");
    h.put("Twi", "tw");
    h.put("Udmurt", "udm");
    h.put("Ugaritic", "uga");
    h.put("Ukrainian", "uk");
    h.put("Umbundu", "umb");
    h.put("Undetermined", "und");
    h.put("Upper Sorbian", "hsb");
    h.put("Urdu", "ur");
    h.put("Uyghur", "ug");
    h.put("Uzbek", "uz");
    h.put("Vai", "vai");
    h.put("Venda", "ve");
    h.put("Vietnamese", "vi");
    h.put("Volapük", "vo");
    h.put("Votic", "vot");
    h.put("Walamo", "wal");
    h.put("Walloon", "wa");
    h.put("Waray-Waray", "war");
    h.put("Washo", "was");
    h.put("Welsh", "cy");
    h.put("West Frisian", "fy");
    h.put("Wolof", "wo");
    h.put("Xhosa", "xh");
    h.put("Yakut", "sah");
    h.put("Yao", "yao");
    h.put("Yapese", "yap");
    h.put("Yiddish", "yi");
    h.put("Yoruba", "yo");
    h.put("Zapotec", "zap");
    h.put("Zazaki", "zza");
    h.put("Zenaga", "zen");
    h.put("Zhuang", "za");
    h.put("Zulu", "zu");
    h.put("Zuni", "zun");
  }

  public static String threeLettersCode(String s) {
    return threeLettersCode(h, s);
  }
}

/**
 *
 */
package org.getalp.dbnary.deu;

import java.util.HashMap;
import org.getalp.LangTools;

/**
 * @author Mariam
 */
public class GermanLangToCode extends LangTools {

  static HashMap<String, String> h = new HashMap<>();

  static {
    h.put("Pro", "pro");
    h.put("Abchasisch", "ab");
    h.put("Acehnesisch", "ace");
    h.put("Adygeisch", "ady");
    h.put("Afar", "aa");
    h.put("Afrikaans", "af");
    h.put("Ägyptisch", "egy");
    h.put("Ainu", "ain");
    h.put("Akan", "ak");
    h.put("Akkadisch", "akk");
    h.put("Albanisch", "sq");
    h.put("Aleutisch", "ale");
    h.put("Altaisch", "alt");
    h.put("Altenglisch", "ang");
    h.put("Altfranzösisch", "fro");
    h.put("Altgriechisch", "grc");
    h.put("Althochdeutsch", "goh");
    h.put("Altirisch", "sga");
    h.put("Altkirchenslawisch", "cu");
    h.put("Altnordisch", "non");
    h.put("Altpersisch", "peo");
    h.put("Amharisch", "am");
    h.put("Arabisch", "ar");
    h.put("Aragonesisch", "an");
    h.put("Aramäisch", "arc");
    h.put("Arawak", "arw");
    h.put("Armenisch", "hy");
    h.put("Aromunisch", "rup");
    h.put("Aserbaidschanisch", "az");
    h.put("Assamesisch", "as");
    h.put("Asturisch", "ast");
    h.put("Avestisch", "ae");
    h.put("Awarisch", "av");
    h.put("Aymara", "ay");
    h.put("Balinesisch", "ban");
    h.put("Bambara", "bm");
    h.put("Baschkirisch", "ba");
    h.put("Baskisch", "eu");
    h.put("Belutschi", "bal");
    h.put("Bemba", "bem");
    h.put("Bengalisch", "bn");
    h.put("Birmanisch", "my");
    h.put("Bislama", "bi");
    h.put("Blackfoot", "bla");
    h.put("Bokmål", "nb");
    h.put("Bosnisch", "bs");
    h.put("Bretonisch", "br");
    h.put("Buginesisch", "bug");
    h.put("Bulgarisch", "bg");
    h.put("Burjatisch", "bua");
    h.put("Cebuano", "ceb");
    h.put("Chamorro", "ch");
    h.put("Cherokee", "chr");
    h.put("Cheyenne", "chy");
    h.put("Chichewa", "ny");
    h.put("Chinesisch", "zh");
    h.put("Chipewyan", "chp");
    h.put("Choctaw", "cho");
    h.put("Cree", "cr");
    h.put("Creek", "mus");
    h.put("Dänisch", "da");
    h.put("Deutsch", "de");
    h.put("Dzongkha", "dz");
    h.put("Englisch", "en");
    h.put("Ersja", "myv");
    h.put("Esperanto", "eo");
    h.put("Estnisch", "et");
    h.put("Ewe", "ee");
    h.put("Färöisch", "fo");
    h.put("Fidschi", "fj");
    h.put("Finnisch", "fi");
    h.put("Fon", "fon");
    h.put("Französisch", "fr");
    h.put("Friaulisch", "fur");
    h.put("Fulfulde", "ff");
    h.put("Galicisch", "gl");
    h.put("Gayo", "gay");
    h.put("Ge’ez", "gez");
    h.put("Georgisch", "ka");
    h.put("Gilbertesisch", "gil");
    h.put("Gotisch", "got");
    h.put("Griechisch (Neu-)", "el");
    h.put("Grönländisch", "kl");
    h.put("Guaraní", "gn");
    h.put("Gudscharati", "gu");
    h.put("Haitianisch", "ht");
    h.put("Hausa", "ha");
    h.put("Hawaianisch", "haw");
    h.put("Hebräisch", "he");
    h.put("Hethitisch", "hit");
    h.put("Hindi", "hi");
    h.put("Hiri Motu", "ho");
    h.put("Iban", "iba");
    h.put("Ido", "io");
    h.put("Igbo", "ig");
    h.put("Ilokano", "ilo");
    h.put("Inarisamisch", "smn");
    h.put("Indonesisch", "id");
    h.put("Inguschisch", "inh");
    h.put("Interlingua", "ia");
    h.put("Interlingue", "ie");
    h.put("Inuktitut", "iu");
    h.put("Inupiak", "ik");
    h.put("Irisch", "ga");
    h.put("isiXhosa", "xh");
    h.put("isiZulu", "zu");
    h.put("Isländisch", "is");
    h.put("Italienisch", "it");
    h.put("Jakutisch", "sah");
    h.put("Japanisch", "ja");
    h.put("Javanisch", "jv");
    h.put("Jiddisch", "yi");
    h.put("Kabardinisch", "kbd");
    h.put("Kabylisch", "kab");
    h.put("Kalmückisch", "xal");
    h.put("Kambodschanisch", "km");
    h.put("Kannada", "kn");
    h.put("Kanuri", "kr");
    h.put("Kapampangan", "pam");
    h.put("Karakalpakisch", "kaa");
    h.put("Karatschai-Balkarisch", "krc");
    h.put("Karelisch", "krl");
    h.put("Kasachisch", "kk");
    h.put("Kaschubisch", "csb");
    h.put("Kashmiri", "ks");
    h.put("Katalanisch", "ca");
    h.put("Kawi", "kaw");
    h.put("Kikamba", "kam");
    h.put("Kikongo", "kg");
    h.put("Kikuyu", "ki");
    h.put("Kinyarwanda", "rw");
    h.put("Kirgisisch", "ky");
    h.put("Kirundi", "rn");
    h.put("Kiswahili", "sw");
    h.put("Klingonisch", "tlh");
    h.put("Komi", "kv");
    h.put("Konkani", "kok");
    h.put("Koreanisch", "ko");
    h.put("Kornisch", "kw");
    h.put("Korsisch", "co");
    h.put("Kosraeanisch", "kos");
    h.put("Krimtatarisch", "crh");
    h.put("Kroatisch", "hr");
    h.put("Kuanyama", "kj");
    h.put("Kumükisch", "kum");
    h.put("Kurdisch", "ku");
    h.put("Ladino", "lad");
    h.put("Laotisch", "lo");
    h.put("Latein", "la");
    h.put("Lettisch", "lv");
    h.put("Limburgisch", "li");
    h.put("Lingala", "ln");
    h.put("Litauisch", "lt");
    h.put("Lojban", "jbo");
    h.put("Luganda", "lg");
    h.put("Luxemburgisch", "lb");
    h.put("Maa", "mas");
    h.put("Madagassisch", "mg");
    h.put("Maduresisch", "mad");
    h.put("Makassar", "mak");
    h.put("Malaiisch", "ms");
    h.put("Malayalam", "ml");
    h.put("Maledivisch", "dv");
    h.put("Maltesisch", "mt");
    h.put("Mandschurisch", "mnc");
    h.put("Manx", "gv");
    h.put("Maori", "mi");
    h.put("Mapudungun", "arn");
    h.put("Marathi", "mr");
    h.put("Mari", "chm");
    h.put("Marshallesisch", "mh");
    h.put("Mazedonisch", "mk");
    h.put("Micmac", "mic");
    h.put("Minangkabau", "min");
    h.put("Mittelenglisch", "enm");
    h.put("Mittelfranzösisch", "frm");
    h.put("Mittelhochdeutsch", "gmh");
    h.put("Mittelirisch", "mga");
    h.put("Mittelniederländisch", "dum");
    h.put("Mizo", "lus");
    h.put("Mohawk", "moh");
    h.put("Mokscha", "mdf");
    h.put("Moldauisch", "mo");
    h.put("Mongolisch", "mn");
    h.put("Nauruisch", "na");
    h.put("Navajo", "nv");
    h.put("Ndonga", "ng");
    h.put("Neapolitanisch", "nap");
    h.put("Nepalesisch", "ne");
    h.put("Newari", "new");
    h.put("Niederdeutsch", "nds");
    h.put("Niederländisch", "nl");
    h.put("Niedersorbisch", "dsb");
    h.put("Niueanisch", "niu");
    h.put("Nogaisch", "nog");
    h.put("Nordfriesisch", "frr");
    h.put("Nordsamisch", "se");
    h.put("Nord-Sotho", "nso");
    h.put("Norwegisch", "no");
    h.put("Nynorsk", "nn");
    h.put("Obersorbisch", "hsb");
    h.put("Ojibwe", "oj");
    h.put("Okzitanisch", "oc");
    h.put("Oriya", "or");
    h.put("Oromo", "om");
    h.put("Osmanisches Türkisch", "ota");
    h.put("Ossetisch", "os");
    h.put("Otjiherero", "hz");
    h.put("Pali", "pi");
    h.put("Pandschabi", "pa");
    h.put("Papiamentu", "pap");
    h.put("Paschtu", "ps");
    h.put("Persisch", "fa");
    h.put("Polnisch", "pl");
    h.put("Portugiesisch", "pt");
    h.put("Quechua", "qu");
    h.put("Rajasthani", "raj");
    h.put("Rapanui", "rap");
    h.put("Rätoromanisch", "rm");
    h.put("Romani", "rom");
    h.put("Rumänisch", "ro");
    h.put("Runyankore", "nyn");
    h.put("Russisch", "ru");
    h.put("Samoanisch", "sm");
    h.put("Sango", "sg");
    h.put("Sanskrit", "sa");
    h.put("Sardisch", "sc");
    h.put("Sasak", "sas");
    h.put("Schottisch-Gälisch", "gd");
    h.put("Schwedisch", "sv");
    h.put("Schweizerdeutsch", "gsw");
    h.put("Scots", "sco");
    h.put("Serbisch", "sr");
    h.put("Serbokroatisch", "sh");
    h.put("Serer", "srr");
    h.put("Setswana", "tn");
    h.put("Shona", "sn");
    h.put("Silozi", "loz");
    h.put("Sindhi", "sd");
    h.put("Singhalesisch", "si");
    h.put("Siswati", "ss");
    h.put("Sizilianisch", "scn");
    h.put("Slowakisch", "sk");
    h.put("Slowenisch", "sl");
    h.put("Somalisch", "so");
    h.put("Soninke", "snk");
    h.put("Spanisch", "es");
    h.put("Sranantongo", "srn");
    h.put("Süd-Ndebele", "nr");
    h.put("Süd-Sotho", "st");
    h.put("Sumerisch", "sux");
    h.put("Sundanesisch", "su");
    h.put("Syrisch", "syr");
    h.put("Tadschikisch", "tg");
    h.put("Tagalog", "tl");
    h.put("Tahitianisch", "ty");
    h.put("Tamaschek", "tmh");
    h.put("Tamil", "ta");
    h.put("Tatarisch", "tt");
    h.put("Telugu", "te");
    h.put("Tetum", "tet");
    h.put("Thailändisch", "th");
    h.put("Tibetisch", "bo");
    h.put("Tigrinya", "ti");
    h.put("Tok Pisin", "tpi");
    h.put("Tokelauisch", "tkl");
    h.put("Tongaisch", "to");
    h.put("Tschechisch", "cs");
    h.put("Tschetschenisch", "ce");
    h.put("Tschuwaschisch", "cv");
    h.put("Tshivenda", "ve");
    h.put("Türkisch", "tr");
    h.put("Turkmenisch", "tk");
    h.put("Tuvaluisch", "tvl");
    h.put("Tuwinisch", "tyv");
    h.put("Udmurtisch", "udm");
    h.put("Uigurisch", "ug");
    h.put("Ukrainisch", "uk");
    h.put("Ungarisch", "hu");
    h.put("Urdu", "ur");
    h.put("Usbekisch", "uz");
    h.put("Vietnamesisch", "vi");
    h.put("Volapük", "vo");
    h.put("Walisisch", "cy");
    h.put("Wallonisch", "wa");
    h.put("Waray", "war");
    h.put("Weißrussisch", "be");
    h.put("Westfriesisch", "fy");
    h.put("Wolof", "wo");
    h.put("Wotisch", "vot");
    h.put("Xitsonga", "ts");
    h.put("Yi", "ii");
    h.put("Yoruba", "yo");
    h.put("Zazaki", "zza");
    h.put("Zenaga", "zen");
    h.put("Zhuang", "za");
  }

  public static String threeLettersCode(String s) {
    return threeLettersCode(h, s);
  }
}

package org.getalp.dbnary.deu;

import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.PropertyObjectPair;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.tools.ArrayMatrix;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/**
 * Created by serasset on 15/01/15.
 */
public abstract class GermanTableExtractorWikiModel extends GermanDBnaryWikiModel {
	protected Logger log = LoggerFactory.getLogger(GermanKonjugationExtractorWikiModel.class);
	protected IWiktionaryDataHandler wdh;

	public GermanTableExtractorWikiModel(WiktionaryIndex wi, Locale locale, String imageBaseURL, String linkBaseURL, IWiktionaryDataHandler wdh) {
		super(wi, locale, imageBaseURL, linkBaseURL);
		this.wdh=wdh;
	}

	protected void parseTables(String declinationTemplateCall){

		Document doc = Jsoup.parse(expandWikiCode(declinationTemplateCall));
		if (null==doc) {
			return ;
		}
		Elements tables = doc.select("table");
		for (Element table : tables) {
            if (table.id().equalsIgnoreCase("toc")) continue;
			parseTable(table);
		}
	}

	protected void parseTable(Element table) {
		Elements rows = table.select("tr");
		ArrayMatrix<String> columnHeaders = new ArrayMatrix<String>();
		int nrow = 0;
		for (Element row : rows) {
			int ncol = 0;
			for (Element cell: row.children()) {
				if (isHeaderCell(cell)) {
					// Advance if current column spans from a previous row
					while (columnHeaders.get(nrow, ncol) != null) ncol++;
					String colspan = cell.attr("colspan");
					String rowspan = cell.attr("rowspan");
					int cspan = (null != colspan && colspan.trim().length() != 0) ? Integer.parseInt(colspan) : 1;
					int rspan = (null != rowspan && rowspan.trim().length() != 0) ? Integer.parseInt(rowspan) : 1;

					for (int i = 0; i < cspan; i++) {
						for (int j = 0; j < rspan; j++) {
							columnHeaders.set(nrow + j, ncol, cell.text());
						}
						ncol++;
					}
				} else if (isNormalCell(cell)) {
					while (columnHeaders.get(nrow, ncol) != null) ncol++;
					String colspan = cell.attr("colspan");
					String rowspan = cell.attr("rowspan");
					int cspan = (null != colspan && colspan.trim().length() != 0) ? Integer.parseInt(colspan) : 1;
					int rspan = (null != rowspan && rowspan.trim().length() != 0) ? Integer.parseInt(rowspan) : 1;
					if (rspan != 1) log.debug("Non null rowspan in data cell ({},{}) for {}", nrow, ncol, wdh.currentLexEntry());

					for (int i = 0; i < cspan; i++) {
						ArrayList<String> context = getRowAndColumnContext(nrow, ncol, columnHeaders);
						GermanInflectionData inflection = getInflectionDataFromCellContext(context);
						if (null != inflection) {
							addForm(inflection.toPropertyObjectMap(), cell.text());
						}
						ncol++;
					}

				} else {
					log.debug("Row child \"{}\"is not a cell in {}", cell.tagName(), wdh.currentLexEntry());
				}
			}
			nrow++;
		}
	}

    protected boolean isNormalCell(Element cell) {
        return cell.tagName().equalsIgnoreCase("td");
    }


    protected boolean isHeaderCell(Element cell) {
        return cell.tagName().equalsIgnoreCase("th");
    }

    protected ArrayList<String> getRowAndColumnContext(int nrow, int ncol, ArrayMatrix<String> columnHeaders) {
		ArrayList<String> res = new ArrayList<>();
		for (int i = 0; i < nrow; i++) {
			String header = columnHeaders.get(i, ncol);
			if (null != header && (header.trim().length() != 0)) res.add(header);
		}
        for (int i = 0; i < ncol; i++) {
            String header = columnHeaders.get(nrow, i);
            if (null != header && (header.trim().length() != 0)) res.add(header);
        }
		return res;
	}

    protected abstract GermanInflectionData getInflectionDataFromCellContext(List<String> context);

	private void addForm(HashSet<PropertyObjectPair> infl, String s){
		s=s.replace("]", "").replace("[", "").replaceAll(".*\\) *", "").replace("(", "").trim();
		if (s.length() == 0 || s.equals("—") || s.equals("-")) return;

		wdh.registerInflection("deu", wdh.currentWiktionaryPos(), s, wdh.currentLexEntry(), 1, infl);
	}
}

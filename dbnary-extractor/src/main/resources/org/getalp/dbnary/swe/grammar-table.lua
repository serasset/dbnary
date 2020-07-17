local lk = require("Modul:link")
local glk = require("Modul:grammar-link")

local export = {}
local m_lang_code

local function getStart(number_of_columns, lang_code, part_of_speech, has_warning, minimize_width)
	local class = minimize_width and "grammar min" or "grammar"
	local str = '{| class="' .. class ..  '" data-lang="' .. lang_code .. '" data-h3="' .. part_of_speech .. '" cellspacing="0" width="10"\n'
	
	if has_warning then
		local talk_page = mw.title.getCurrentTitle().talkPageTitle.fullText
		str = str .. '|class="grammar-warning min" colspan="' .. number_of_columns .. '"|Faktakoll: Innehåller ifrågasatta uppgifter. Se [[' .. talk_page .. '|diskussion]].\n'
	end

	return str
end

function export.getStart(number_of_columns, lang_code, part_of_speech, has_warning)
	return getStart(number_of_columns, lang_code, part_of_speech, has_warning, false)
end
	
function export.getMinimalStart(number_of_columns, lang_code, part_of_speech, has_warning)
	return getStart(number_of_columns, lang_code, part_of_speech, has_warning, true)
end

function export.getHiddenStart(number_of_columns, lang_code, part_of_speech, has_warning)
	return "" --not implemented
end

function export.setLanguage(lc)
	m_lang_code = lc
end

function export.getRow(...)
	local row = "|-\n"
	local cell = {}
	local default_meta = "|"
	local default_type = "infl"
	local has_empty_cell = false
	
	local function makeContentNonEmpty(str)
		return (type(str) ~= "string" or str == "" or str == " ") and "&nbsp;" or str
	end

	local function isHeading(meta)
		return mw.ustring.sub(meta, 1, 1) == "!"
	end

	local function addCell()
		local extended_content = ""

		if isHeading(cell.meta) then
			extended_content = makeContentNonEmpty(cell.content)
		else
			extended_content = glk.link(cell.content, cell.type, m_lang_code)
			if extended_content == "&nbsp;" then
				has_empty_cell = true
				extended_content = extended_content .. "[[Kategori:Wiktionary:Sidor med tomma celler]]"
			end
		end
		
		row = row .. cell.meta .. "|" .. extended_content .. "\n"
	end

	local arg = {...}

	for _,v in pairs(arg) do
		if type(v) == "table" then
		  -- print(table.concat(v, "+++") .. " is a table")
			cell.meta = v[1]
			cell.content = v[2]
			cell.type = v["type"] or default_type
			addCell()
		elseif type(v) == "string" then
		  -- print(v .. " is a string")
			cell.meta = default_meta
			cell.content = v
			cell.type = default_type
			addCell()
		end
	end
	
	return row
end

function export.getEnd(number_of_columns, note, as_first_part)
	local str = ""
	local note_exists = note ~= "-"

	if as_first_part then
		str = str .. '|-\n!colspan="' .. number_of_columns .. '" class="min"|' .. "''Som förled i sammansättningar används " .. (lk.isValidLinkTarget(as_first_part) and "'''" .. as_first_part .. "-'''" or as_first_part) .. "''.\n"
	end

	if note_exists then
		str = str .. '|-\n|colspan="' .. number_of_columns .. '" class="note"|' .. "<div><table><tr><th>Not:</th><td>" .. note .. "</td></tr></table></div>\n"
	end

	str = str .. '|}' 

	return str
end

return export

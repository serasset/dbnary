local export = {}

-- Stubbing checkparams as it is only used for debugging inside wiktionary
function export.warn(frame)
	return ""
end

function export.error(frame)
  return ""
end

return export

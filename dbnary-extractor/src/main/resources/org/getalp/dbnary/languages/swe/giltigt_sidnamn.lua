-- Kopierad från https://en.wikipedia.org/wiki/Module:IsValidPageName (sidan under tiden raderad där)
-- Används av {{giltigt sidnamn}}

local export = {}

function export.isValidPageName(frame)
  -- The bliki implementation of title does not check for well formed titles, so just
  -- catch the most usefull glitches
  -- Check if arg contains any of the characters []{}|#<>
  if string.find(frame.args[1], "[%[%]{}|#]") then
      return ""
  else
      return "valid"
  end
--	local success, res = pcall(mw.title.new, frame.args[1])
--	if success and res then
--		return "valid"
--	else
--		return ""
--	end
end

return export
package com.emot.androidclient.util;

import gnu.inet.encoding.Stringprep;
import gnu.inet.encoding.StringprepException;
import android.content.Context;
import android.text.Editable;
import android.util.TypedValue;

import com.emot.androidclient.exceptions.EmotXMPPAdressMalformedException;

public class XMPPHelper {

	public static String verifyJabberID(String jid)
			throws EmotXMPPAdressMalformedException {
		try {
			String parts[] = jid.split("@");
			if (parts.length != 2 || parts[0].length() == 0 || parts[1].length() == 0)
				throw new EmotXMPPAdressMalformedException(
						"Configured Jabber-ID is incorrect!");
			StringBuilder sb = new StringBuilder();
			sb.append(Stringprep.nodeprep(parts[0]));
			sb.append("@");
			sb.append(Stringprep.nameprep(parts[1]));
			return sb.toString();
		} catch (StringprepException spe) {
			throw new EmotXMPPAdressMalformedException(spe);
		} catch (NullPointerException e) {
			throw new EmotXMPPAdressMalformedException("Jabber-ID wasn't set!");
		}
	}

	public static String verifyJabberID(Editable jid)
			throws EmotXMPPAdressMalformedException {
		return verifyJabberID(jid.toString());
	}
	
	public static int tryToParseInt(String value, int defVal) {
		int ret;
		try {
			ret = Integer.parseInt(value);
		} catch (NumberFormatException ne) {
			ret = defVal;
		}
		return ret;
	}

	public static String capitalizeString(String original) {
		return (original.length() == 0) ? original :
			original.substring(0, 1).toUpperCase() + original.substring(1);
	}

	public static int getEditTextColor(Context ctx) {
		TypedValue tv = new TypedValue();
		boolean found = ctx.getTheme().resolveAttribute(android.R.attr.editTextColor, tv, true);
		if (found) {
			// SDK 11+
			return ctx.getResources().getColor(tv.resourceId);
		} else {
			// SDK < 11
			return ctx.getResources().getColor(android.R.color.primary_text_light);
		}
	}
}

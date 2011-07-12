/*
*  XTAPI JTapi implementation
*  Copyright (C) 2002 Steven A. Frare
* 
*  This program is free software; you can redistribute it and/or
*  modify it under the terms of the GNU General Public License
*  as published by the Free Software Foundation; either version 2
*  of the License, or (at your option) any later version.
*  
*  This program is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU General Public License for more details.
*  
*  You should have received a copy of the GNU General Public License
*  along with this program; if not, write to the Free Software
*  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*
 * @author  Steven A. Frare
 * @version .01
 */

#include <windows.h>
#define TAPI_CURRENT_VERSION 0x00010004
#include <tapi.h>
#include <stdio.h>

#include "net_xtapi_ServiceProvider_MSTAPI.h"

extern void debugString(int iSev, const char * module, const char * logMsg, const char * where);
extern void initSound(int);
extern void teardownSound();

// Acknowledgement.:
// _where() macro from http://gcc.gnu.org
#define _wh_(_file, _line)  _file ":" #_line
#define __wh_(file, line) _wh_(file, line)
#define _where() __wh_(__FILE__, __LINE__)


/**
 * This file is generated by xresloader 2.7.0, please don't edit it.
 * You can find more information about this xresloader on https://xresloader.atframe.work/ .
 * If there is any problem, please find or report issues on https://github.com/xresloader/xresloader/issues .
 */
#include "Config/ArrInArr.h"



UArrInArrHelper::UArrInArrHelper() : Super()
{
    UArrInArrHelper::ClearRow(this->Empty);
}

void UArrInArrHelper::ClearRow(FArrInArr& TableRow)
{
    TableRow.Name = TEXT("");
    TableRow.IntArr.Reset(0);
    TableRow.StrArr.Reset(0);
}

void UArrInArrHelper::ClearDataRow(FArrInArr& TableRow) const
{
    UArrInArrHelper::ClearRow(TableRow);
}


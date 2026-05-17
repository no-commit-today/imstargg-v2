"use client";

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import type { FeedbackItem, FeedbackListResponse } from "@/lib/types";
import { FeedbackCheckToggle } from "./FeedbackCheckToggle";
import { FeedbackContentCell } from "./FeedbackContentCell";

const CATEGORY_BADGE_CLASS: Record<FeedbackItem["category"], string> = {
  BUG: "bg-red-100 text-red-800 border-transparent",
  FEATURE_REQUEST: "bg-blue-100 text-blue-800 border-transparent",
  ETC: "bg-neutral-100 text-neutral-700 border-transparent",
};

function formatDate(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => n.toString().padStart(2, "0");
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

interface Props {
  data: FeedbackListResponse;
}

export function FeedbackTable({ data }: Props) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead className="w-12">체크</TableHead>
          <TableHead className="w-36">유형</TableHead>
          <TableHead className="w-36">작성일시</TableHead>
          <TableHead>내용</TableHead>
          <TableHead className="w-48">이메일</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {data.content.map((row) => (
          <TableRow key={row.id}>
            <TableCell>
              <FeedbackCheckToggle id={row.id} initialChecked={row.checked} />
            </TableCell>
            <TableCell>
              <Badge className={CATEGORY_BADGE_CLASS[row.category]}>
                {row.category}
              </Badge>
            </TableCell>
            <TableCell title={row.createdAt}>{formatDate(row.createdAt)}</TableCell>
            <TableCell>
              <FeedbackContentCell content={row.content} />
            </TableCell>
            <TableCell>
              {row.email ? (
                <a className="underline" href={`mailto:${row.email}`}>
                  {row.email}
                </a>
              ) : (
                "-"
              )}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}

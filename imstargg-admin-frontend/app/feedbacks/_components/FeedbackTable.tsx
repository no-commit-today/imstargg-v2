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
import type { FeedbackItem, FeedbackListResponse, FeedbackRating } from "@/lib/types";
import { FeedbackCheckToggle } from "./FeedbackCheckToggle";
import { FeedbackContentCell } from "./FeedbackContentCell";

const RATING_LABEL: Record<FeedbackRating, string> = {
  VERY_SATISFIED: "매우 만족",
  SATISFIED: "만족",
  NEUTRAL: "보통",
  DISSATISFIED: "불만족",
  VERY_DISSATISFIED: "매우 불만족",
};

const RATING_EMOJI: Record<FeedbackRating, string> = {
  VERY_SATISFIED: "😄",
  SATISFIED: "🙂",
  NEUTRAL: "😐",
  DISSATISFIED: "🙁",
  VERY_DISSATISFIED: "😡",
};

const RATING_BADGE_CLASS: Record<FeedbackRating, string> = {
  VERY_SATISFIED: "bg-green-100 text-green-800 border-transparent",
  SATISFIED: "bg-lime-100 text-lime-800 border-transparent",
  NEUTRAL: "bg-neutral-100 text-neutral-700 border-transparent",
  DISSATISFIED: "bg-orange-100 text-orange-800 border-transparent",
  VERY_DISSATISFIED: "bg-red-100 text-red-800 border-transparent",
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
          <TableHead className="w-36">만족도</TableHead>
          <TableHead className="w-36">작성일시</TableHead>
          <TableHead>내용</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {data.content.map((row: FeedbackItem) => (
          <TableRow key={row.id}>
            <TableCell>
              <FeedbackCheckToggle id={row.id} initialChecked={row.checked} />
            </TableCell>
            <TableCell>
              <Badge className={RATING_BADGE_CLASS[row.rating]}>
                <span className="mr-1" aria-hidden>{RATING_EMOJI[row.rating]}</span>
                {RATING_LABEL[row.rating]}
              </Badge>
            </TableCell>
            <TableCell title={row.submittedAt}>{formatDate(row.submittedAt)}</TableCell>
            <TableCell>
              <FeedbackContentCell content={row.content} />
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}

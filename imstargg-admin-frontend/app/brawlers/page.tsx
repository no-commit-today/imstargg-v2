"use client";

import { useEffect, useState } from "react";
import { AdminLayout } from "../admin-layout";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { getBrawlers } from "@/lib/api";
import { ImageThumbnail } from "@/components/image-thumbnail";
import type { BrawlerListItem } from "@/lib/types";
import Link from "next/link";

export default function BrawlerListPage() {
  const [brawlers, setBrawlers] = useState<BrawlerListItem[]>([]);

  useEffect(() => {
    getBrawlers().then(setBrawlers);
  }, []);

  return (
    <AdminLayout>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-2xl font-bold">브롤러 관리</h2>
        <Link href="/brawlers/new">
          <Button>브롤러 등록</Button>
        </Link>
      </div>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>BrawlStars ID</TableHead>
            <TableHead>이름 (KO)</TableHead>
            <TableHead>이름 (EN)</TableHead>
            <TableHead>Rarity</TableHead>
            <TableHead>Role</TableHead>
            <TableHead>이미지</TableHead>
            <TableHead></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {brawlers.map((b) => (
            <TableRow key={b.brawlStarsId}>
              <TableCell>{b.brawlStarsId}</TableCell>
              <TableCell>{b.names.KOREAN || "-"}</TableCell>
              <TableCell>{b.names.ENGLISH || "-"}</TableCell>
              <TableCell>{b.rarity}</TableCell>
              <TableCell>{b.role}</TableCell>
              <TableCell><ImageThumbnail url={b.imageUrl} alt={b.names.KOREAN || String(b.brawlStarsId)} /></TableCell>
              <TableCell>
                <Link href={`/brawlers/${b.brawlStarsId}`}>
                  <Button variant="outline" size="sm">상세</Button>
                </Link>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </AdminLayout>
  );
}

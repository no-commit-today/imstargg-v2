"use client";

import { Fragment } from "react";
import { diffLines } from "diff";

const CONTEXT_LINES = 3;

interface ApiSpecDiffViewProps {
  previousContent: string;
  content: string;
}

export function ApiSpecDiffView({ previousContent, content }: ApiSpecDiffViewProps) {
  const changes = diffLines(previousContent, content);

  if (changes.length === 1 && !changes[0].added && !changes[0].removed) {
    return <p className="text-sm text-neutral-500">이전 스냅샷과 차이가 없습니다.</p>;
  }

  return (
    <pre className="overflow-x-auto rounded-md border bg-muted/30 p-4 text-xs leading-5">
      {changes.map((change, i) => {
        const lines = change.value.split("\n");
        // split 결과 마지막의 빈 문자열(트레일링 개행) 제거
        if (lines[lines.length - 1] === "") lines.pop();

        if (change.added) {
          return (
            <Fragment key={i}>
              {lines.map((line, j) => (
                <div key={j} className="bg-green-100 text-green-900 dark:bg-green-950 dark:text-green-200">
                  {`+ ${line}`}
                </div>
              ))}
            </Fragment>
          );
        }
        if (change.removed) {
          return (
            <Fragment key={i}>
              {lines.map((line, j) => (
                <div key={j} className="bg-red-100 text-red-900 dark:bg-red-950 dark:text-red-200">
                  {`- ${line}`}
                </div>
              ))}
            </Fragment>
          );
        }

        // 변경 없는 블록은 앞뒤 컨텍스트만 남기고 접는다
        if (lines.length <= CONTEXT_LINES * 2 + 1) {
          return (
            <Fragment key={i}>
              {lines.map((line, j) => (
                <div key={j} className="text-neutral-500">{`  ${line}`}</div>
              ))}
            </Fragment>
          );
        }
        const head = i === 0 ? [] : lines.slice(0, CONTEXT_LINES);
        const tail = i === changes.length - 1 ? [] : lines.slice(-CONTEXT_LINES);
        const omitted = lines.length - head.length - tail.length;
        return (
          <Fragment key={i}>
            {head.map((line, j) => (
              <div key={`h${j}`} className="text-neutral-500">{`  ${line}`}</div>
            ))}
            <div className="my-1 text-center text-neutral-400">⋯ {omitted}줄 생략 ⋯</div>
            {tail.map((line, j) => (
              <div key={`t${j}`} className="text-neutral-500">{`  ${line}`}</div>
            ))}
          </Fragment>
        );
      })}
    </pre>
  );
}

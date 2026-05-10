'use client'
import { use } from 'react'
import NoteForm from '../../../components/NoteForm'

export default function EditNotePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params)
  return <NoteForm id={id} />
}

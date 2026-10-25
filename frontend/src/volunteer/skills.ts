import type { Skill } from '../api/types'

const categoryLabel = (category: Skill['category']) => category.charAt(0) + category.slice(1).toLowerCase()

// MultiSelect data grouped by category, groups and skills in alphabetical order (Q73).
export function skillOptions(skills: Skill[]) {
  const groups = new Map<string, { value: string; label: string }[]>()
  for (const skill of [...skills].sort((a, b) => a.skillName.localeCompare(b.skillName))) {
    const group = categoryLabel(skill.category)
    groups.set(group, [...(groups.get(group) ?? []), { value: String(skill.id), label: skill.skillName }])
  }
  return [...groups.entries()].sort(([a], [b]) => a.localeCompare(b)).map(([group, items]) => ({ group, items }))
}

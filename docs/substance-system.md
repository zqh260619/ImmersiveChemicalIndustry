# 物质表示系统设计（Substance Model）

> 本文是 `docs/game-progression.md` §7「实现路线图」中"物质注册表"的具体设计。
> 约束来源：五条铁律（物质真实、无魔法、从零自举、纯度即进度、历史即逻辑）；技术栈 Minecraft 1.21.1 / Fabric（Data Components、Fabric Transfer API）。

---

## 0. 设计目标

用一个**数据驱动**的模型统一表示现实中的各类物质，使"物品 / 流体 / 气体 / 混合物 / 合金"共用同一套定义，并让纯度、杂质、手性、活性等质量属性成为第一等公民。

三条硬约束：

1. **物质 = 定义（Substance）**，与"载体"（物品、流体、方块）解耦；
2. **质量 = 数据组件（Quality）**，任何载体都能携带并比较；
3. **数量 = 物理量（mol / g / mB）**，反应、配方、分析统一换算，不出现"魔法单位"。

---

## 1. 三层模型

```
Substance（物质定义，JSON）
   ├─ 元素 / 化合物 / 混合物 / 合金
   ├─ 物性：摩尔质量、密度、熔点、沸点、溶解度
   ├─ 危险：腐蚀、毒性、易燃、爆炸、氧化、放射
   └─ 标签：acid / base / solvent / ore / alloy / explosive …
        │
        ▼  按 form 生成载体
Form（形态）
   ├─ powder / crystal / lump / ingot / nugget / block / sheet → 物品
   └─ liquid / gas → 流体（+ 容器）
        │
        ▼  每个实例携带
Quality（质量，Data Component）
   ├─ purity、impurities、ee、moisture、grain、activity、assay、isotope
   └─ 分析仪器读它；研究手册记它；配方校验它
```

- **同一物质不同纯度 = 不同堆叠**（组件不同）；
- **同一物质不同形态 = 不同物品**（`sodium_hydroxide_powder` / `sodium_hydroxide`(流体)）；
- **混合物/合金 = 一个物质（composition 字段）**，例如矿石、粗盐、煤焦油馏分、青铜；
- **反应/分离中的不定配比物料 = 过程流（§4.5），不是物质**：只有被"认定"后才成为物质，否则只能分离、分析或废弃。

---

## 2. 物质定义（Substance）

### 2.1 数据位置与命名

- JSON 路径：`data/immersivechemicalindustry/substances/<id>.json`
- 物质 id：英文 snake_case，化学名优先（`sulfuric_acid`、`sodium_hydroxide`、`chalcopyrite`、`bronze`、`charcoal`）
- 物品 id：`<substance_id>_<form>`（`iron_ingot`、`chalcopyrite_ore_lump`、`sodium_hydroxide_powder`）
- 流体 id：`<substance_id>`（`sulfuric_acid`、`oxygen`、`nitrogen`）
- 命名空间统一 `immersivechemicalindustry:`；原版同类物品（铁锭、铜锭、矿石）被本系统取代（见 §8）。

### 2.2 字段表

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | string | 唯一键 |
| `name` | {zh_cn, en_us} | 显示名（lang 由 datagen 生成） |
| `kind` | enum | `element` / `compound` / `mixture` / `alloy` |
| `formula` | string? | 分子式（混合物可空） |
| `molar_mass` | double? | g/mol（化合物/元素必填） |
| `composition` | map? | 混合物/合金组成：`{substance: fraction}`；合金可用 `{min, nominal, max}` |
| `forms` | list | 物理形态列表（见 2.3） |
| `thermal` | {melt_k, boil_k, decomp_k?} | 相变温度（K） |
| `density_g_ml` | double | 密度（液体/固体） |
| `solubility` | map? | `{water: miscible/soluble/insoluble}` |
| `hazard` | {corrosive, toxic, flammable, explosive, oxidizing, radioactive, chronic} | 0–3 等级，驱动 UI 与安全机制 |
| `tags` | list | `acid` / `base` / `solvent` / `fuel` / `ore` / `mineral` / `alloy` / `explosive` / `food_grade` … |
| `quality_axes` | list | 该物质有意义的质检维度：`assay`（品位/含量）、`purity`、`impurities`、`ee`、`moisture`、`grain`（粒径）、`activity`（酶活）、`isotope` |
| `unit_mass_g` / `mol_per_item` | map? | 覆盖默认物品单位质量或单件摩尔数（见 §5） |
| `notes` | string? | 手册用注解（如"浓硫酸可钝化碳钢"） |

### 2.3 forms 字段

```json
{"form": "liquid", "state": "liquid", "color": "#e8e0c8", "density_g_ml": 1.84,
 "melt_k": 283.2, "boil_k": 610.0, "unit_mass_g": null}
```

| form | 载体 | 计量方式 | 合成表关系 | 例 |
|---|---|---|---|---|
| `powder` | 物品 | 纯物质：`mol_per_item`（默认 1 mol）；混合物：`unit_mass_g` | — | 氧化铁粉、草木灰碱 |
| `crystal` | 物品 | 同上 | — | 硫酸铜晶体、白糖 |
| `nugget` | 物品 | 1/9 基数 | 9 粒 = 1 锭 | 铁粒、锡粒 |
| `ingot` | 物品 | 1 基数（纯金属默认 1 mol） | 9 锭 = 1 块 | 铁锭、青铜锭 |
| `block` | 方块物品 | 9 基数 | — | 铁块、玻璃块 |
| `lump` | 物品 | 混合物：`unit_mass_g`（默认 100 g） | — | 石灰石块、矿石块 |
| `sheet` | 物品 | 纯金属：1 mol | — | 铜板、钢板 |
| `liquid` | 流体 | mB + 密度 → mol | 1 桶 = 1000 mB ≈ 1 L | 硫酸、粗油、液氮 |
| `gas` | 流体 | mB + 密度 → mol | 钢瓶按质量装载 | 氧气、氢气、氯气 |

### 2.4 示例一：硫酸（化合物 + 液体）

```json
{
  "id": "sulfuric_acid",
  "name": {"zh_cn": "硫酸", "en_us": "Sulfuric Acid"},
  "kind": "compound",
  "formula": "H2SO4",
  "molar_mass": 98.079,
  "forms": [{"form": "liquid", "state": "liquid", "color": "#e6dcbc", "density_g_ml": 1.84,
             "melt_k": 283.2, "boil_k": 610.0}],
  "solubility": {"water": "miscible"},
  "hazard": {"corrosive": 3, "toxic": 2},
  "tags": ["acid", "inorganic", "strong_acid"],
  "quality_axes": ["purity", "impurities", "moisture"],
  "notes": "浓硫酸可钝化碳钢；稀硫酸腐蚀碳钢。"
}
```

> 浓度处理：**溶液 = 混合物物质**（如 `dilute_sulfuric_acid_65` 的 composition 含 water），不把"98%/65%"塞进同一个物质的 purity；purity 只表示"该物质内部杂质"。这样与真实化学一致，也避免"浓度藏在质量里"的歧义。**已确认：v1 采用预定义浓度档**（98%/65%/稀硫酸分别为独立物质），动态 composition 留待 v2。

### 2.5 示例二：黄铜矿矿石（混合物 + 品位）

```json
{
  "id": "chalcopyrite",
  "name": {"zh_cn": "黄铜矿", "en_us": "Chalcopyrite"},
  "kind": "compound",
  "formula": "CuFeS2",
  "molar_mass": 183.52,
  "forms": [],
  "hazard": {"toxic": 1},
  "tags": ["mineral", "sulfide"],
  "quality_axes": ["purity"]
}
```

```json
{
  "id": "chalcopyrite_ore",
  "name": {"zh_cn": "黄铜矿矿石", "en_us": "Chalcopyrite Ore"},
  "kind": "mixture",
  "composition": {"chalcopyrite": 0.02, "gangue": 0.98},
  "forms": [{"form": "lump", "state": "solid", "color": "#c8a24a",
             "density_g_ml": 3.2, "unit_mass_g": 100}],
  "tags": ["ore", "sulfide_ore"],
  "quality_axes": ["assay"]
}
```

> **选矿就是改 composition**：破碎 → 浮选 → 精矿（`chalcopyrite_concentrate`，chalcopyrite 0.25）+ 尾矿。这给阶段 1/2 的选矿提供统一的数学生成机制。

### 2.6 示例三：青铜（合金 + 组成公差）

```json
{
  "id": "bronze",
  "name": {"zh_cn": "青铜", "en_us": "Bronze"},
  "kind": "alloy",
  "composition": {
    "copper": {"min": 0.80, "nominal": 0.88, "max": 0.93},
    "tin":    {"min": 0.07, "nominal": 0.12, "max": 0.20}
  },
  "forms": [{"form": "ingot", "state": "solid", "color": "#b08040",
             "density_g_ml": 8.8, "unit_mass_g": 100}],
  "tags": ["alloy", "copper_alloy"],
  "quality_axes": ["composition", "impurities"]
}
```

> 组成公差决定性能：成分偏离 nominal ±5% → 硬度下降（阶段 1 的"配比试错"）；钢的碳含量（0.2–0.8% 为合格）同理。**合金的 composition 是质量属性，不是不同物质**。

### 2.7 示例四：木炭（混合物 + 固定碳）

```json
{
  "id": "charcoal",
  "name": {"zh_cn": "木炭", "en_us": "Charcoal"},
  "kind": "mixture",
  "composition": {"carbon": 0.85, "ash": 0.10, "volatile_matter": 0.05},
  "forms": [{"form": "lump", "state": "solid", "color": "#2b2b2b",
             "density_g_ml": 0.4, "unit_mass_g": 50}],
  "tags": ["fuel", "reducing_agent", "adsorbent"],
  "quality_axes": ["fixed_carbon", "ash", "volatiles"]
}
```

> 阶段 0 的"敲击声/无烟燃烧"判定可以落在 `fixed_carbon` 阈值上，而不是另造一个"品质"字段——质检维度就是游戏机制。

---

## 3. 形态与载体

### 3.1 固体 → 物品（自动生成）

每个 `(substance, form)` 组合自动注册一个物品：

```java
Registry.register(Registries.ITEM, id(substanceId + "_" + form),
    new Item(new Item.Settings()
        .component(ModComponents.SUBSTANCE, substanceId)
        .component(ModComponents.QUALITY, Quality.pristine(substance))
        .maxCount(form.maxStack())));
```

- 物品组按 `kind` 分配：`ici_elements` / `ici_inorganics` / `ici_organics` / `ici_ores` / `ici_materials`；
- 堆叠上限：粉末 64、晶体 64、锭 16、块 1（沿用原版手感）；
- 质量组件使不同纯度的同物质**不能混堆**（合理且可解释）。

### 3.2 液体 / 气体 → 流体 + 容器

- 流体注册：`Fluids.STILL`（及 `FLOWING`）；气体用 `state: gas` 标记，容器按"气瓶"处理；
- 容器（阶段 0–3 由简到繁）：
  - **玻璃瓶 / 安瓿**：实验室级容器，携带 `FluidVariant` + 质量组件（腐蚀品需要专用瓶：玻璃怕 HF、PTFE 衬里等，见总纲 §2.5）；
  - **桶**：1000 mB 通用容器（原版桶逻辑复用，但只能装被允许的流体）；
  - **储罐（多方块）**：1000 L 级，带材质/压力/温度上限；
  - **钢瓶**：气体专用，带压力等级（阶段 5+）。
- 流体实例携带 `composition` / `quality` 组件；容器之间由 Fabric `Storage<FluidVariant>` 传递，混合规则按体积加权（v1 允许混合，v2 可加"禁止混合"约束）。

### 3.3 容器与材质

容器是独立物品/方块（玻璃瓶、陶罐、铅罐、不锈钢罐、PTFE 衬里罐），其"可装介质"由材质决定（总纲 §2.5 的腐蚀规则），不需要每个物质单独做容器。

---

## 4. 质量属性（Data Components）

### 4.1 组件定义

```java
public record Quality(
    double purity,                        // 0..1，物质自身纯度
    Map<String, Double> impurities,       // 杂质物质 id → 分数
    Map<String, Double> composition,      // 混合物/合金实际组成（覆盖定义中的 nominal）
    double ee,                            // 手性 ee（1.0 = 外消旋? no：ee=0 是外消旋，ee=1 是单一对映体）
    Map<String, Double> extras            // assay / moisture / grain / activity / isotope …
)
```

- `purity`：单物质内部杂质总量（1 − purity = 杂质总量，逐项记录在 `impurities`）；
- `composition`：混合物/合金的实测组成（选矿、配比、精馏的结果）；
- `ee`：手性物质的对映体过量（0 = 外消旋，1 = 纯对映体）；
- `extras`：可扩展的质检项（品位 assay、含水 moisture、粒径 grain、酶活 activity U/mg、同位素 isotopology）。

### 4.2 默认值与来源

- 新物品默认 `purity = 1.0`（理论纯物质）或少量的工业默认值（矿石按 composition + assay 给）；
- 质量**只能由真实过程产生**：结晶提纯、精馏、萃取、电解、重结晶、洗涤——即"纯度即进度"。

### 4.3 未分析 = 未知（知识闸门）

- 物品带 `analyzed` 标记（组件内布尔）。
- 未分析时 tooltip 只显示物质名 + 形态 + "未检定"，不显示纯度/杂质；
- 用滴定/熔点/折光/GC/HPLC/NMR 分析后写入实测值，tooltip 显示"检定值 ± 误差"，并记入研究手册；
- 这就在机制层面实现了"没有仪器就没有知识"。

### 4.4 危险属性

`hazard` 字段驱动 tooltip 图标与处理规则：

- `corrosive ≥2`：徒手操作受伤；需要耐腐蚀容器/手套；
- `toxic ≥2`：不戴防毒面具会中毒；泄漏需要通风/洗涤塔；
- `flammable/explosive`：与火源、静电、浓度（爆炸极限）联动；
- `radioactive`：剂量率与铅屏蔽（稀土/铀矿链）。

---

## 4.5 动态混合物：过程流（Stream）

### 4.5.1 分界：物质 vs 过程流

- **物质（Substance）**：有稳定名义组成、有行业名称、可作为货物/原料独立买卖的实体。混合物也可以注册为物质（矿石、合金、粗盐、黑灰、煤焦油馏分），但组成是**名义值 + 公差**。
- **过程流（Process Stream）**：反应釜、蒸馏塔、储罐里"此刻实际是什么"。它没有名义组成，只有**实测组成**；它不是物质，不注册为物质。

不能把反应混合物注册成物质的理由：

1. **组合爆炸**：产物 × 未反应物 × 副产物 × 溶剂 × 催化剂 → 无穷组合；
2. **没有稳定 id**：不定配比无法复现，注册后配方/JEI 语义会崩；
3. **本质是过程状态**：反应液带温度、压力、批次、溯源，属于"过程"，不属于"物性"；
4. **真实世界没有干净产物**：未反应原料与副产物必须留在物料里，由分离与循环处理——这正是硬核玩法的一部分。

### 4.5.2 Stream 数据模型

```java
record ProcessStream(
    Phase phase,                   // LIQUID | GAS | SLURRY | SOLID_MIX
    Map<String, Double> fluidMol,  // 液相/气相组分（含溶解气体、均相催化剂）
    Map<String, Double> solidMol,  // 悬浮/固体组分（含固体催化剂）
    double liquidVolumeMl,         // 体积（mB；气相共用此字段）
    double temperatureK,
    double pressureKpa,
    String provenance              // 反应 id / 批次 id（可选，供手册与事故追责）
)
```

- 组分一律 **mol**（4 位有效数字），质量与体积由密度/摩尔质量导出；
- **未反应物、副产物、溶剂、杂质全部在同一张组分表里**；
- 与 §4.1 `Quality.composition` 的分工：`composition` 用于**已认定物质**（矿石品位、合金组成、浓度档），`Stream` 用于**未认定/不定的实际物料**（反应液、精馏中间馏分、萃取相、母液）。
- **v1 相态范围（已确认）**：单液相 + 气相 + 悬浮固体（`SLURRY`）；不建模多液层——萃取/分液用"分配系数 + 第二个 stream"表达，而不是在一个 stream 里放多层液体。

### 4.5.3 载体

| 载体 | 实现 | 例 |
|---|---|---|
| 储罐 / 管道 / 反应釜 | BlockEntity 存 `ProcessStream` | 合成氨循环回路、精馏塔 |
| 玻璃瓶 / 桶 / 安瓿 | 容器物品 + stream 数据组件（maxCount 1） | 实验室取样、手动分液 |
| 固体混合物 | 通用物品 `process_mix`（携带 `solidMol`，不可进配方） | 反应残渣、滤饼 |

- 容器物品的 stream 组件让"取样—分析—转移"成为真实操作；
- `process_mix` 不是物质，只是载体；玩家必须先分离或认定才能当原料用。
- **已确认**：早期容器（玻璃瓶/桶/安瓿）支持携带任意 stream，支撑阶段 0–3 的手工实验（取液、分液、过滤）。
- **`ProcessStream` 没有独立物品形态**：它只存在于三类位置——① 设备 BlockEntity（反应釜、蒸馏塔、储罐、离心机）；② 容器物品的数据组件（玻璃瓶、桶、安瓿、取样管）；③ 固体混合物料载体 `process_mix`。因此 stream 可以随容器物品进入背包、被拿起/放下/倾倒/取样，但玩家永远不会捡到"裸的 stream 物品"；容器物品不堆叠（maxCount 1），且受材质与温度限制（腐蚀性、高温液体会损坏容器）。

### 4.5.4 认定：Stream → 物质

当 stream 组成落在某定义混合物的**公差**内，且玩家用分析仪器取得证据（滴定/密度/折光/GC…），可"认定"为该物质：

- 例：苯硝化后的酸相 ≈ 硫酸 70% + 硝酸 5% + 水 25%，若匹配"硝化混酸"物质公差，可认定；
- **认定不等于自动交易**：认定只是允许它进入配方与交易体系；未认定的 stream 只能分离或废弃；
- 反向也成立：把 98% 硫酸倒入水中 → 稀释热 + 变成 stream（不再是可以直接用的"物质"）。

### 4.5.5 分离是一等操作

反应 → stream → 分离 → 分析 → 认定/循环 → 产物。

| 操作 | 输入 → 输出 | v1 简化 |
|---|---|---|
| 过滤 / 离心 | 浆料 → 滤饼 + 母液 | 粒径截留阈值 |
| 分液 / 倾析 | 液-液 → 两相 stream | 互溶度与密度差 |
| 蒸馏 / 精馏 | 液 → 馏分 + 釜残 | 理想气液平衡，按沸点切割 |
| 减压蒸馏 | 液 → 馏分 + 残液 | 真空降沸点（安托万方程） |
| 萃取 | 液 + 溶剂 → 萃取相 + 萃余相 | 固定分配系数（v2 支持 pH 依赖） |
| 结晶 / 重结晶 | 液 → 晶体 + 母液 | 溶解度曲线 |
| 洗涤 | 固体 + 洗液 → 净化固体 + 洗涤液 | 按溶解损失计算 |
| 干燥 / 蒸发 | 液/湿固体 → 固体 + 蒸汽 stream | 蒸发潜热 |
| 电解 / 电精炼 | 液 → 阴极产物 + 阳极泥 | 电化学序列（阶段 5+） |

- 每个操作消耗时间与能量，产出新 stream；**未反应物回到反应器**——这是阶段 4 合成氨"未反应气循环"与阶段 6"联产平衡"的教学入口。

### 4.5.6 未分析 = 未知

- 未分析（已确认）：只显示相态、体积/质量、"未知混合物"，以及**肉眼可得的观察**（颜色、分层、沉淀、结晶、气味等级）；
- 分析后：显示组分表（mol/质量/百分比），仪器等级决定误差与检出限，并记入研究手册；
- 早期没有仪器时只能靠分层/沉淀/颜色判断——这正是化学史的开端。

### 4.5.7 容量与性能

- 组分表上限 **64 种（已确认）**；超出提示玩家先分离（不自动合并，避免真实性损失）；
- 数值 4 位有效数字；低于检出限的痕量可丢弃（但影响纯度判定，需谨慎）；
- 同步：容器物品用组件同步，储罐用 BlockEntity 更新包（只同步变化量与截断组分）；
- stream 不进入工作台配方；只有认定后的物质能进配方体系。

### 4.5.8 示例：苯硝化后的混合物流

```
反应：苯 + 混酸 → 硝基苯 + 水 + 间二硝基苯（副产物） + 未反应苯 + 过量酸
Stream（液相 1 L，60 °C）：
  硝基苯 0.62 mol | 苯 0.05 mol | 硝酸 0.02 mol | 硫酸 0.30 mol | 水 0.01 mol | 间二硝基苯 0.002 mol

操作 1 分液   → 有机相（硝基苯 + 苯 + 副产物）/ 酸相（硝酸 + 硫酸 + 水）
操作 2 酸相   → 浓缩回收 → 回到硝化（循环）
操作 3 有机相 → 碱洗 → 蒸馏 → 硝基苯（商品）+ 釜残
每一步都需要分析确认（滴定酸度、GC 组成、折光/密度）
```

---

## 5. 单位制（已确认：mol 优先，兼容合成表）

**总原则**：纯物质以 **mol** 为主计量；混合物以 **质量（g）** 为主；物品个数通过 `mol_per_item`（纯物质）或 `unit_mass_g`（混合物）换算；合成表沿用原版的 9:1 关系（1 块 = 9 锭 = 81 粒）。

| 量 | 内部单位 | 换算 |
|---|---|---|
| 物质的量（纯物质） | mol | 质量 / 摩尔质量；物品：个数 × `mol_per_item` |
| 物质的量（混合物） | 由组成推得 | 质量 × 各组分分数 / 各组分摩尔质量 |
| 质量 | g | 密度 × 体积 |
| 体积 | mB | 1 桶 ≈ 1000 mB ≈ 1 L |
| 物品个数 | 个 | 纯物质 1 个 = 1 mol（可配 `mol_per_item`）；块/锭/粒按 9:1:1/9 基数 |

**合成表兼容（关键例子）**——铁：

| 形态 | 个数关系 | 物质的量 | 质量 |
|---|---|---|---|
| 铁粒 | 9 粒 = 1 锭 | 1/9 mol | ≈ 6.2 g |
| 铁锭 | 1 锭（基数） | 1 mol | ≈ 55.8 g |
| 铁块 | 9 锭 = 1 块 | 9 mol | ≈ 502 g |

1 铁块 = 9 铁锭 = 81 铁粒，与原版合成表一致，同时 mol 是整数基数——两边都不牺牲。

**其他例子**：

- 生石灰 CaO：1 个粉末 = 1 mol ≈ 56.1 g；
- 硫酸（液体）：1 mB ≈ 1.84 g ≈ 0.0188 mol；1 桶（1000 mB）≈ 18.8 mol；
- 黄铜矿矿石（混合物）：1 块 = 100 g，含 2% CuFeS₂ ⇒ 约 0.011 mol 铜（理论值，实际按选矿品位算）；
- 气体（氧气/氢气）：按 mB 与密度换算，钢瓶按质量装载（避免"1 mol 气体涨满容器"的荒谬感）。

**书写规则**：

- 反应与工艺公式用 mol；
- 物品/工作台配方用个数；
- UI 同时显示两种表述：`需要 1 mol H₂SO₄ ≈ 98 g ≈ 1 瓶（53 mL）`；
- 密度、摩尔质量、沸点全部来自物质定义，反应引擎不硬编码。

---

## 6. 自动生成管线

启动时（或 datapack 重载时）：

1. 扫描 `data/immersivechemicalindustry/substances/*.json` → `SubstanceRegistry`（校验 id 唯一、composition 引用存在、molar_mass 与 formula 一致）；
2. 对每个物质 × form：
   - 固体：注册物品 + 加入物品组；
   - 液体/气体：注册流体（`Fluid` + 必要的 `BucketItem`/瓶装物品）；
3. 客户端着色：每种形态用一张灰度底图，按 `color` 做 `ItemColorProvider` 着色（不逐物质手绘图）；
4. datagen：从 JSON 的 `name` 生成 `zh_cn` / `en_us` 词条、物品模型、标签（`#ici:powders`、`#ici:ores`、`#ici:acids`…）；
5. 规模估算：早期 100–200 种物质 × 2–3 形态 ≈ 300–600 物品，可控；全流程预计 1500–2500 个注册项，需要注册名压缩策略（见 §9 性能）。

---

## 7. 与其他系统的接口

### 7.1 反应引擎

```json
{
  "id": "sulfuric_acid_contact",
  "inputs": [
    {"substance": "sulfur_dioxide", "mol": 2},
    {"substance": "oxygen", "mol": 1}
  ],
  "conditions": {"temp_k_range": [673, 773], "pressure_kpa_range": [100, 300],
                 "catalyst": "vanadium_pentoxide", "min_catalyst_purity": 0.9},
  "outputs": [
    {"substance": "sulfur_trioxide", "mol": 2,
     "yield": "f(temp, pressure, catalyst_purity, purity_of_inputs)"}
  ],
  "safety": {"explosive": 0}
}
```

- 收率函数读取输入纯度、温度/压力偏差、催化剂状态——**纯度和条件直接进入数学**；
- 反应引擎只认 `substance + mol`，不认物品 id；
- **反应输出一律是 `ProcessStream`**（§4.5）：即使主产物明确，也含未反应物、副产物与溶剂；分离与认定后才成为物质物品。

### 7.2 分析

- 仪器读取 `Quality` 组件（或流体变体的质量组件），按仪器等级加入噪声与检限；
- 输出"检定值"写回组件（`analyzed=true`），并解锁手册条目；
- 谱图（NMR/IR/GC）由物质定义 + 质量生成特征峰列表（不逐物质手写全谱）。

### 7.3 研究手册

手册按 `substance.tags` 与反应 id 组织条目；只有"验证过"的物质/反应才显示方程与参数（知识闸门）。

### 7.4 机器与容器

所有机器输入/输出用 `substance + mol` 描述，容器用 `Storage<ItemVariant>` / `Storage<FluidVariant>`，不直接写物品 id。

---

## 8. 原版内容映射

- 删除原版矿物生成与熔炼配方（总纲 §5）；铁/铜/金/锡等金属改用本系统 `iron`、`copper` 等物质；
- 原版 `minecraft:iron_ingot` 等仍存在（部分配方/结构需要），但**配方链由本系统接管**；
- 红石全系移除，不进入物质系统；
- 水作为特殊物质（`water`）保留，纯净度影响反应（蒸馏水/去离子水/超纯水是不同质量档，不是不同物质）。

---

## 9. 实现骨架（类与文件）

```
substance/
  Substance.java          // record + Codec（§2 字段）
  SubstanceForm.java      // powder / crystal / … / liquid / gas
  Composition.java        // 组成与公差
  Hazard.java
  SubstanceRegistry.java  // JSON 加载、校验、查询
  SubstanceItems.java     // 自动注册物品（§3.1）
  SubstanceFluids.java    // 自动注册流体（§3.2）
  SubstanceTags.java      // 标签常量
quality/
  Quality.java            // record + Codec
  ModComponents.java      // SUBSTANCE / QUALITY / ANALYZED 组件类型
analysis/
  AnalysisContext.java    // 仪器读数模型
```

- 组件注册用 `DataComponentType.Builder` + `Codec`；物品 `Item.Settings().component(...)`；
- JSON 由 `Codec` 解析，缺字段给默认值，解析失败在启动日志列出文件与原因；
- v1 用模组内置资源（jar 内 JSON）保证客户端/服务端一致；v2 再支持 datapack 与重载同步包。

**性能注意**：物质数量上千后，注册项与语言键会膨胀；策略是"物质 JSON 只写一次，物品/流体/语言/模型全部由管线生成"，且物品按需生成（`forms` 为空则不生成物品）。

---

## 10. 分期

| 期 | 内容 | 验收 |
|---|---|---|
| P1 | 物质 JSON + 注册表 + 固体物品自动生成 + Quality 组件 + tooltip | 阶段 0 的物质（黏土、燧石、石灰石、生石灰、熟石灰、木炭、草木灰、盐、水）能以物品存在且带质量组件 |
| P2 | 流体 + 瓶/桶/储罐 + 组合与混合 | 硫酸/盐酸/液氮能装瓶、装桶、倒出 |
| P3 | 反应引擎 + 分析仪器读取 | 一个真实反应（如石灰石煅烧）由数据驱动，产物纯度可检定 |

> **实现时机（已确认）**：暂不实现。等阶段 0 等实际代码开工时一并评估，届时允许按实现难度与"是否符合实际"调整本设计（例如气体储运、混合规则、注册项压缩策略），调整后必须回写本文档。

---

## 11. 已确认的取舍

| 取舍 | 决定 |
|---|---|
| 物品粒度 | **每个物质每个形态独立物品**（`<substance>_<form>`）；配方、JEI、贴图直观；全流程预计 1500–2500 个注册项 |
| 计量 | **mol 优先**（纯物质 1 个 = 1 mol），同时兼容合成表 9:1:81 关系（块/锭/粒基数倍率）；混合物按质量 + 组成换算 |
| 溶液 | **v1 预定义浓度档**（浓度档即独立混合物物质）；动态 composition 留 v2 |
| 过程流 | **v1 单液相 + 气相 + 悬浮固体**；萃取/分液用分配系数 + 第二 stream 表达，不建模多液层；未分析只显示相态/体积/未知 + 肉眼观察；组分上限 64，超限提示；早期容器支持任意 stream |
| 实现时机 | **延后**，实际实现时再按难度与真实性调整并回写文档 |

遗留待定（实现时确认）：

- 气体储运细节（钢瓶压力等级与装载上限）；
- 注册项压缩策略、datapack 与客户端同步时机；
- 痕量成分丢弃规则（低于检出限时是否从组分表删除）。
